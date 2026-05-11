const API_BASE = "https://umg1.duckdns.org/kiosko/api";

const params = new URLSearchParams(window.location.search);
const camaraId = params.get("camaraId") || localStorage.getItem("camaraId") || "1";

localStorage.setItem("camaraId", camaraId);

const video = document.getElementById("video");
const canvas = document.getElementById("canvas");
const carnetInput = document.getElementById("carnet");
const btnValidar = document.getElementById("btnValidar");
const infoCurso = document.getElementById("infoCurso");
const resultado = document.getElementById("resultado");

const TIEMPO_RESET = 30000;
const TIEMPO_REFRESH = 1800000;

let temporizadorReset = null;
let procesando = false;

document.addEventListener("DOMContentLoaded", () => {
    iniciarCamara();
    cargarSesion();
    inicializarEventosKiosko();

    carnetInput.focus();
});

function inicializarEventosKiosko() {
    btnValidar.addEventListener("click", verificarAsistencia);

    carnetInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            verificarAsistencia();
        }
    });

    document.addEventListener("click", reiniciarTemporizador);
    document.addEventListener("keydown", reiniciarTemporizador);

    document.addEventListener("click", activarPantallaCompleta, { once: true });

    video.addEventListener("error", async () => {
        console.log("Error de video. Reiniciando camara...");
        await iniciarCamara();
    });

    reiniciarTemporizador();

    setInterval(() => {
        location.reload();
    }, TIEMPO_REFRESH);

    setInterval(async () => {
        if (!video.srcObject) {
            console.log("Reconectando camara...");
            await iniciarCamara();
        }
    }, 10000);

    setInterval(() => {
        carnetInput.focus();
    }, 2000);
}

async function iniciarCamara() {
    try {
        if (video.srcObject) {
            video.srcObject.getTracks().forEach(track => track.stop());
            video.srcObject = null;
        }

        const stream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: false
        });

        video.srcObject = stream;

    } catch (error) {
        mostrarResultado("No se pudo acceder a la camara.", false);
    }
}

async function cargarSesion() {
    try {
        const response = await fetch(`${API_BASE}/sesion-activa/${camaraId}`);
        const data = await response.json();

        if (data.success) {
            infoCurso.innerHTML = `
                <strong>Curso:</strong> ${data.curso}<br>
                <strong>Catedratico:</strong> ${data.catedratico}<br>
                <strong>Ubicacion:</strong> ${data.puerta}<br>
                <strong>Camara:</strong> ${data.camara}
            `;
        } else {
            infoCurso.innerHTML = "No hay sesion activa para esta camara.";
        }

    } catch (error) {
        infoCurso.innerHTML = "Error cargando sesion activa.";
    }
}

async function verificarAsistencia() {
    if (procesando) return;

    const carnet = carnetInput.value.trim();

    if (!carnet) {
        mostrarResultado("Ingrese o escanee un carnet.", false);
        return;
    }

    try {
        procesando = true;
        btnValidar.disabled = true;

        mostrarResultado("Procesando validacion facial...", null);

        const imagen = capturarImagen();

        const response = await fetch(`${API_BASE}/verificar-asistencia`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                camaraId: camaraId,
                carnet: carnet,
                imagen: imagen
            })
        });

        const data = await response.json();

        if (data.success) {
            mostrarResultado(`
                <div class="result-icon">✅</div>
                <div>ASISTENCIA REGISTRADA</div>
                <small>${data.nombre}</small>
                <small>${data.carnet}</small>
                <small>${data.curso}</small>
                <small>Confianza facial: ${data.confianza}%</small>
            `, true);
        } else {
            mostrarResultado(`
                <div class="result-icon">❌</div>
                <div>${data.mensaje || "No se pudo registrar la asistencia."}</div>
            `, false);
        }

        carnetInput.value = "";
        carnetInput.focus();
        reiniciarTemporizador();

    } catch (error) {
        mostrarResultado("Error de conexion con el servidor.", false);
    } finally {
        procesando = false;
        btnValidar.disabled = false;
    }
}

function capturarImagen() {
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;

    const ctx = canvas.getContext("2d");
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    return canvas.toDataURL("image/jpeg", 0.9);
}

function mostrarResultado(mensaje, success) {
    resultado.style.display = "flex";
    resultado.className = "resultado";

    if (success === true) {
        resultado.classList.add("success");
    } else if (success === false) {
        resultado.classList.add("error");
    } else {
        resultado.classList.add("loading");
    }

    resultado.innerHTML = mensaje;
}

function reiniciarKiosko() {
    if (procesando) return;

    carnetInput.value = "";
    resultado.innerHTML = "";
    resultado.style.display = "none";
    carnetInput.focus();

    cargarSesion();
}

function reiniciarTemporizador() {
    if (temporizadorReset) {
        clearTimeout(temporizadorReset);
    }

    temporizadorReset = setTimeout(() => {
        reiniciarKiosko();
    }, TIEMPO_RESET);
}

async function activarPantallaCompleta() {
    try {
        if (!document.fullscreenElement) {
            await document.documentElement.requestFullscreen();
        }
    } catch (e) {
        console.log("Fullscreen bloqueado.");
    }
}