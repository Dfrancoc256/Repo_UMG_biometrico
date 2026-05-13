// ══════════════════════════════════════════════
//  CONFIGURACIÓN DEL KIOSKO
// ══════════════════════════════════════════════
const API_BASE = 'https://umg1.duckdns.org'; // ← cambia por tu IP/dominio

let camaraId   = null;
let puertaNombre = '';
let sesionActiva = null;
let streamCam  = null;
let personaActual = null;

// ── Reloj ──────────────────────────────────────
setInterval(() => {
    document.getElementById('reloj').textContent =
        new Date().toLocaleTimeString('es-GT');
}, 1000);

// ══════════════════════════════════════════════
//  INICIO — verificar si ya está configurado
// ══════════════════════════════════════════════
window.addEventListener('DOMContentLoaded', () => {
    const idGuardado = localStorage.getItem('kiosko_camara_id');
    if (idGuardado) {
        camaraId = parseInt(idGuardado);
        puertaNombre = localStorage.getItem('kiosko_puerta_nombre') || 'Acceso';
        document.getElementById('lblPuerta').textContent = puertaNombre;
        document.getElementById('modalConfig').style.display = 'none';
        iniciarKiosko();
    } else {
        cargarPuertas();
        document.getElementById('modalConfig').style.display = 'flex';
    }
});

// ══════════════════════════════════════════════
//  MODAL — configuración inicial
// ══════════════════════════════════════════════
function cargarPuertas() {
    fetch(API_BASE + '/kiosko/api/puertas-camaras')
        .then(r => r.json())
        .then(data => {
            const sel = document.getElementById('selectPuerta');
            sel.innerHTML = '<option value="">-- Selecciona --</option>';
            data.forEach(item => {
                const opt = document.createElement('option');
                opt.value       = item.camaraId;
                opt.dataset.nombre = item.nombre;
                opt.textContent = item.nombre;
                sel.appendChild(opt);
            });
        })
        .catch(() => {
            document.getElementById('selectPuerta').innerHTML =
                '<option value="">Error al cargar puertas</option>';
        });
}

function guardarConfiguracion() {
    const sel = document.getElementById('selectPuerta');
    const id  = sel.value;
    const nombre = sel.options[sel.selectedIndex]?.dataset.nombre;
    if (!id) { alert('Selecciona una puerta'); return; }

    localStorage.setItem('kiosko_camara_id',      id);
    localStorage.setItem('kiosko_puerta_nombre',  nombre);
    camaraId     = parseInt(id);
    puertaNombre = nombre;

    document.getElementById('lblPuerta').textContent = puertaNombre;
    document.getElementById('modalConfig').style.display = 'none';
    iniciarKiosko();
}

// ══════════════════════════════════════════════
//  INICIAR KIOSKO
// ══════════════════════════════════════════════
function iniciarKiosko() {
    cargarSesionActiva();
    setInterval(cargarSesionActiva, 30000); // refresca cada 30 seg
}

function cargarSesionActiva() {
    fetch(API_BASE + '/kiosko/api/sesion-activa/' + camaraId)
        .then(r => r.json())
        .then(data => {
            const badge = document.getElementById('sesionBadge');
            const info  = document.getElementById('infoCurso');

            if (data.success) {
                sesionActiva = data;
                badge.className = 'sesion-badge activa';
                badge.textContent = '● Sesión activa';
                info.innerHTML =
                    `<i class="fas fa-book-open"></i>
           <strong>${data.curso}</strong> &nbsp;·&nbsp;
           ${data.catedratico} &nbsp;·&nbsp;
           <span class="puerta-tag">${data.puerta}</span>`;
            } else {
                sesionActiva = null;
                badge.className = 'sesion-badge';
                badge.textContent = 'Sin sesión activa';
                info.innerHTML =
                    '<i class="fas fa-info-circle"></i> No hay sesión activa en este acceso.';
            }
        })
        .catch(() => {
            document.getElementById('infoCurso').innerHTML =
                '<i class="fas fa-exclamation-triangle"></i> Sin conexión al servidor';
        });
}

// ══════════════════════════════════════════════
//  PASO 1 — Buscar por carnet
// ══════════════════════════════════════════════
function validarCarnet() {
    const num = document.getElementById('carnet').value.trim();
    if (!num) { mostrarResultado('Ingresa el número de carnet', 'error'); return; }

    const carnet = 'UMG-' + num.toUpperCase();
    mostrarResultado('<i class="fas fa-spinner fa-spin"></i> Buscando...', 'info');

    fetch(API_BASE + '/personas/buscar-carnet?carnet=' + encodeURIComponent(carnet))
        .then(r => r.json())
        .then(data => {
            if (!data.encontrado) {
                mostrarResultado('No se encontró el carnet: ' + carnet, 'error');
                return;
            }
            personaActual = data;
            mostrarPaso2(data);
        })
        .catch(() => mostrarResultado('Error de conexión', 'error'));
}

// ══════════════════════════════════════════════
//  PASO 2 — Mostrar datos + encender cámara
// ══════════════════════════════════════════════
function mostrarPaso2(data) {
    document.getElementById('paso1').style.display = 'none';
    document.getElementById('paso2').style.display = 'block';
    document.getElementById('resultado').innerHTML  = '';

    document.getElementById('fotoEstudiante').src = API_BASE + data.fotoUrl;
    document.getElementById('nombreEstudiante').textContent = data.nombre;
    document.getElementById('carnetEstudiante').textContent = data.carnet;
    document.getElementById('tipoEstudiante').textContent   = data.tipo || '';

    encenderCamara();
}

function encenderCamara() {
    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
        .then(stream => {
            streamCam = stream;
            const video = document.getElementById('video');
            video.srcObject = stream;
        })
        .catch(err => {
            mostrarResultado('No se pudo acceder a la cámara: ' + err.message, 'error');
        });
}

// ══════════════════════════════════════════════
//  CAPTURAR Y VERIFICAR 1:1
// ══════════════════════════════════════════════
function capturarYVerificar() {
    if (!personaActual) return;

    const btn = document.getElementById('btnCapturar');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Verificando...';
    document.getElementById('estadoVerificacion').innerHTML =
        '<i class="fas fa-spinner fa-spin"></i> Procesando imagen...';

    const video  = document.getElementById('video');
    const canvas = document.getElementById('canvas');
    canvas.width  = video.videoWidth  || 640;
    canvas.height = video.videoHeight || 480;
    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
    const imagenBase64 = canvas.toDataURL('image/jpeg', 0.9);

    const payload = {
        camaraId:   camaraId,
        carnet:     personaActual.carnet,
        imagen:     imagenBase64
    };

    fetch(API_BASE + '/kiosko/api/verificar-asistencia', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(r => r.json())
        .then(data => {
            apagarCamara();

            if (data.success) {
                document.getElementById('estadoVerificacion').innerHTML =
                    '<span style="color:#28a745"><i class="fas fa-check-circle"></i> Identidad verificada</span>';

                mostrarResultado(
                    `<div class="resultado-ok">
          <i class="fas fa-check-circle"></i>
          <div>
            <strong>${data.nombre}</strong>
            <span>${data.carnet}</span>
            <span>${data.curso}</span>
            <span>Confianza: ${data.confianza}%</span>
            <span><i class="fas fa-door-open"></i> ${puertaNombre}</span>
          </div>
        </div>`, 'ok'
                );

                setTimeout(reiniciar, 4000);
            } else {
                document.getElementById('estadoVerificacion').innerHTML =
                    '<span style="color:#dc3545"><i class="fas fa-times-circle"></i> No coincide</span>';

                mostrarResultado(
                    `<i class="fas fa-times-circle"></i> ${data.mensaje || 'Rostro no coincide'} ` +
                    `(${data.confianza ?? 0}%)`, 'error'
                );

                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-camera"></i> Intentar de nuevo';
                encenderCamara();
            }
        })
        .catch(err => {
            mostrarResultado('Error de conexión: ' + err.message, 'error');
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-camera"></i> Capturar y Verificar';
        });
}

// ══════════════════════════════════════════════
//  UTILIDADES
// ══════════════════════════════════════════════
function reiniciar() {
    apagarCamara();
    personaActual = null;
    document.getElementById('carnet').value    = '';
    document.getElementById('paso1').style.display = 'block';
    document.getElementById('paso2').style.display = 'none';
    document.getElementById('resultado').innerHTML  = '';
    document.getElementById('estadoVerificacion').innerHTML =
        '<i class="fas fa-eye"></i> Mira directo a la cámara...';
    const btn = document.getElementById('btnCapturar');
    btn.disabled = false;
    btn.innerHTML = '<i class="fas fa-camera"></i> Capturar y Verificar';
}

function apagarCamara() {
    if (streamCam) {
        streamCam.getTracks().forEach(t => t.stop());
        streamCam = null;
    }
}

function mostrarResultado(msg, tipo) {
    const el = document.getElementById('resultado');
    const clases = { ok: 'resultado-ok', error: 'resultado-error', info: 'resultado-info' };
    el.className = 'resultado ' + (clases[tipo] || '');
    el.innerHTML = msg;
}

// ── Bitácora ──────────────────────────────────
let todosLosIngresos = [];

// Cargar puertas en el selector
function cargarPuertasFiltro() {
    fetch('/ingreso/api/puertas-todas')
        .then(r => r.json())
        .then(puertas => {
            const sel = document.getElementById('filtroPuerta');
            puertas.forEach(p => {
                const opt = document.createElement('option');
                opt.value = p.id;
                opt.textContent = p.nombre;
                sel.appendChild(opt);
            });
        });
}

function cargarBitacora() {
    const puertaId = document.getElementById('filtroPuerta').value;
    const url = '/ingreso/api/recientes?limit=100' + (puertaId ? '&puertaId=' + puertaId : '');

    fetch(url)
        .then(r => r.json())
        .then(ingresos => {
            todosLosIngresos = ingresos;
            renderBitacora(ingresos);
        })
        .catch(() => {});
}

function filtrarBitacora() {
    const q = document.getElementById('filtroBusqueda').value.toLowerCase().trim();
    if (!q) { renderBitacora(todosLosIngresos); return; }
    const filtrados = todosLosIngresos.filter(i =>
        (i.nombre  || '').toLowerCase().includes(q) ||
        (i.carnet  || '').toLowerCase().includes(q)
    );
    renderBitacora(filtrados);
}

function renderBitacora(ingresos) {
    const lista = document.getElementById('listaIngresos');

    if (!ingresos || ingresos.length === 0) {
        lista.innerHTML =
            '<div style="text-align:center;color:var(--color-text-secondary);padding:24px 12px;">' +
            '<i class="fas fa-clipboard-list" style="font-size:20px;opacity:0.3;display:block;margin-bottom:8px;"></i>' +
            '<p style="font-size:11px;margin:0;">Sin registros</p></div>';
        return;
    }

    lista.innerHTML = '';
    ingresos.forEach(i => {
        const hora = i.fechaHora
            ? new Date(i.fechaHora).toLocaleTimeString('es-GT',
                {hour:'2-digit', minute:'2-digit', second:'2-digit'})
            : '—';

        const item = document.createElement('div');
        item.style.cssText =
            'padding:10px 14px;border-bottom:0.5px solid var(--color-border-tertiary);';
        item.innerHTML =
            '<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:2px;">' +
            '<p style="font-size:12px;font-weight:500;color:var(--color-text-primary);margin:0;">'
            + (i.nombre || '—') + '</p>' +
            '<span style="background:#1b5e20;color:white;padding:1px 7px;border-radius:10px;font-size:10px;">'
            + (i.confianza ?? '—') + '%</span>' +
            '</div>' +
            '<p style="font-size:10px;color:var(--color-text-secondary);margin:0;">' +
            '<i class="fas fa-clock" style="margin-right:3px;opacity:0.6;"></i>' + hora +
            ' · <i class="fas fa-door-open" style="margin:0 3px;opacity:0.6;"></i>' + (i.puerta || i.tipo || '—') +
            (i.curso ? ' · ' + i.curso : '') +
            '</p>';
        lista.appendChild(item);
    });
}

// Inicializar bitácora al cargar
cargarPuertasFiltro();
cargarBitacora();
setInterval(cargarBitacora, 15000); // refresca cada 15s