const API_BASE = window.location.origin;
const params = new URLSearchParams(window.location.search);
const camaraId = params.get("camaraId");

let sesionActiva = null;
let personaActual = null;
let streamCam = null;
let intentosRostro = 0;

const MAX_INTENTOS = 3;

window.addEventListener("DOMContentLoaded", () => {
    document.getElementById("modalConfig").style.display = "none";

    if (!camaraId) {
        mostrarResultado("No se recibió camaraId en la URL.", "error");
        return;
    }

    iniciarReloj();
    cargarSesionActiva();

    document.getElementById("carnet").focus();

    setInterval(() => document.getElementById("carnet").focus(), 1500);
    setInterval(cargarSesionActiva, 30000);
});

function iniciarReloj() {
    setInterval(() => {
        document.getElementById("reloj").textContent =
            new Date().toLocaleTimeString("es-GT");
    }, 1000);
}

function cargarSesionActiva() {
    fetch(`${API_BASE}/kiosko/api/sesion-activa/${camaraId}`)
        .then(r => r.json())
        .then(data => {
            const badge = document.getElementById("sesionBadge");
            const info = document.getElementById("infoCurso");
            const puerta = document.getElementById("lblPuerta");

            if (data.success) {
                sesionActiva = data;
                badge.className = "sesion-badge activa";
                badge.textContent = "● Sesión activa";
                puerta.textContent = data.puerta || "Acceso";

                info.innerHTML = `
                    <i class="fas fa-book-open"></i>
                    <strong>${data.curso}</strong> ·
                    ${data.catedratico} ·
                    <span class="puerta-tag">${data.puerta}</span>
                `;
            } else {
                sesionActiva = null;
                badge.className = "sesion-badge";
                badge.textContent = "Sin sesión activa";
                info.innerHTML = `<i class="fas fa-info-circle"></i> No hay sesión activa para esta cámara.`;
            }
        })
        .catch(() => {
            mostrarResultado("Sin conexión con el servidor.", "error");
        });
}

function validarCarnet() {
    if (!sesionActiva) {
        mostrarResultado("No hay sesión activa para esta cámara.", "error");
        reiniciarPronto();
        return;
    }

    const numero = document.getElementById("carnet").value.trim();

    if (!numero) {
        mostrarResultado("Ingrese su carnet.", "error");
        return;
    }

    const carnet = numero.toUpperCase().startsWith("UMG-")
        ? numero.toUpperCase()
        : "UMG-" + numero.toUpperCase();

    mostrarResultado(`<i class="fas fa-spinner fa-spin"></i> Buscando carnet...`, "info");

    fetch(`${API_BASE}/personas/buscar-carnet?carnet=${encodeURIComponent(carnet)}`)
        .then(r => r.json())
        .then(data => {
            if (!data.encontrado) {
                mostrarResultado("Carnet no encontrado.", "error");
                reiniciarPronto();
                return;
            }

            personaActual = data;
            intentosRostro = 0;
            mostrarPasoRostro(data);
        })
        .catch(() => {
            mostrarResultado("Error buscando el carnet.", "error");
            reiniciarPronto();
        });
}

function mostrarPasoRostro(data) {
    document.getElementById("paso1").style.display = "none";
    document.getElementById("paso2").style.display = "block";
    document.getElementById("resultado").innerHTML = "";

    document.getElementById("fotoEstudiante").src = data.fotoUrl ? API_BASE + data.fotoUrl : "";
    document.getElementById("nombreEstudiante").textContent = data.nombre;
    document.getElementById("carnetEstudiante").textContent = data.carnet;
    document.getElementById("tipoEstudiante").textContent = data.tipo || "";

    encenderCamara().then(() => {
        setTimeout(capturarYVerificar, 1200);
    });
}

function encenderCamara() {
    return navigator.mediaDevices.getUserMedia({ video: { facingMode: "user" }, audio: false })
        .then(stream => {
            streamCam = stream;
            document.getElementById("video").srcObject = stream;
        })
        .catch(err => {
            mostrarResultado("No se pudo acceder a la cámara: " + err.message, "error");
            reiniciarPronto();
        });
}

function capturarYVerificar() {
    if (!personaActual) return;

    intentosRostro++;

    const estado = document.getElementById("estadoVerificacion");
    estado.innerHTML = `<i class="fas fa-spinner fa-spin"></i> Verificando rostro... intento ${intentosRostro}/${MAX_INTENTOS}`;

    const video = document.getElementById("video");
    const canvas = document.getElementById("canvas");

    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    canvas.getContext("2d").drawImage(video, 0, 0, canvas.width, canvas.height);

    const imagen = canvas.toDataURL("image/jpeg", 0.9);

    fetch(`${API_BASE}/kiosko/api/verificar-asistencia`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            camaraId: camaraId,
            carnet: personaActual.carnet,
            imagen: imagen
        })
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                mostrarResultado(`
                    <div class="resultado-ok">
                        <i class="fas fa-check-circle"></i>
                        <div>
                            <strong>Asistencia registrada</strong>
                            <span>${data.nombre}</span>
                            <span>${data.carnet}</span>
                            <span>${data.curso}</span>
                            <span>Confianza: ${data.confianza}%</span>
                        </div>
                    </div>
                `, "ok");

                setTimeout(reiniciar, 3500);
                return;
            }

            if (intentosRostro < MAX_INTENTOS) {
                estado.innerHTML = `<i class="fas fa-eye"></i> Coloque mejor su rostro. Intento ${intentosRostro + 1}/${MAX_INTENTOS}`;
                setTimeout(capturarYVerificar, 1800);
            } else {
                mostrarResultado("No se pudo verificar el rostro. Intente nuevamente desde el inicio.", "error");
                setTimeout(reiniciar, 3000);
            }
        })
        .catch(() => {
            mostrarResultado("Error de conexión al verificar rostro.", "error");
            reiniciarPronto();
        });
}

function reiniciar() {
    apagarCamara();

    personaActual = null;
    intentosRostro = 0;

    document.getElementById("carnet").value = "";
    document.getElementById("paso1").style.display = "flex";
    document.getElementById("paso2").style.display = "none";
    document.getElementById("resultado").innerHTML = "";
    document.getElementById("estadoVerificacion").innerHTML =
        `<i class="fas fa-eye"></i> Mira directo a la cámara...`;

    document.getElementById("carnet").focus();
}

function reiniciarPronto() {
    setTimeout(reiniciar, 2500);
}

function apagarCamara() {
    if (streamCam) {
        streamCam.getTracks().forEach(t => t.stop());
        streamCam = null;
    }
}

function mostrarResultado(msg, tipo) {
    const el = document.getElementById("resultado");
    const clases = {
        ok: "resultado-ok",
        error: "resultado-error",
        info: "resultado-info"
    };

    el.className = "resultado " + (clases[tipo] || "");
    el.innerHTML = msg;
}