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
const MAX_INTENTOS = 3;

let temporizadorReset = null;
let procesando = false;
let intentos = 0;
let carnetActual = null;

document.addEventListener("DOMContentLoaded", () => {
    iniciarCamara();
    cargarSesion();
    inicializarEventosKiosko();
    carnetInput.focus();
});

function inicializarEventosKiosko() {
    btnValidar.addEventListener("click", iniciarValidacion);

    carnetInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") iniciarValidacion();
    });

    document.addEventListener("click", reiniciarTemporizador);
    document.addEventListener("keydown", reiniciarTemporizador);
    document.addEventListener("click", activarPantallaCompleta, { once: true });

    video.addEventListener("error", async () => {
        await iniciarCamara();
    });

    reiniciarTemporizador();

    setInterval(() => location.reload(), TIEMPO_REFRESH);

    setInterval(async () => {
        if (!video.srcObject) await iniciarCamara();
    }, 10000);

    setInterval(() => carnetInput.focus(), 2000);
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
        mostrarResultado("No se pudo acceder a la cámara.", false);
    }
}

async function cargarSesion() {
    try {
        const response = await fetch(`${API_BASE}/sesion-activa/${camaraId}`);
        const data = await response.json();

        if (data.success) {
            infoCurso.innerHTML = `
                <strong>Curso:</strong> ${data.curso}<br>
                <strong>Catedrático:</strong> ${data.catedratico}<br>
                <strong>Ubicación:</strong> ${data.puerta}<br>
                <strong>Cámara:</strong> ${data.camara}
            `;
        } else {
            infoCurso.innerHTML = "No hay sesión activa para esta cámara.";
        }

    } catch (error) {
        infoCurso.innerHTML = "Error cargando sesión activa.";
    }
}

function iniciarValidacion() {
    if (procesando) return;

    let carnet = carnetInput.value.trim();

    if (!carnet.startsWith("UMG-")) {
        carnet = "UMG-" + carnet;
    }

    if (!carnet) {
        mostrarResultado("Ingrese o escanee un carnet.", false);
        return;
    }

    carnetActual = carnet;
    intentos = 0;
    procesando = true;
    btnValidar.disabled = true;

    activarScanner(true);

    mostrarResultado("Coloque su rostro frente a la cámara...", null);

    setTimeout(capturarYVerificar, 900);
}

async function capturarYVerificar() {
    if (!carnetActual) return;

    intentos++;
    activarScanner(true);

    try {
        mostrarResultado(`Verificando rostro... intento ${intentos}/${MAX_INTENTOS}`, null);

        const imagen = capturarImagen();

        const response = await fetch(`${API_BASE}/verificar-asistencia`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                camaraId: camaraId,
                carnet: carnetActual,
                imagen: imagen
            })
        });

        const data = await response.json();

        activarScanner(false);

        if (data.success) {
            mostrarResultado(`
                <div class="result-icon">✅</div>
                <div>ASISTENCIA REGISTRADA</div>
                <small>${data.nombre}</small>
                <small>${data.carnet}</small>
                <small>${data.curso}</small>
                <small>Confianza facial: ${data.confianza}%</small>
            `, true);

            setTimeout(reiniciarKiosko, 3500);
            return;
        }

        if (intentos < MAX_INTENTOS) {
            mostrarResultado(
                `Rostro no reconocido. Coloque mejor su rostro. Intento ${intentos}/${MAX_INTENTOS}`,
                null
            );

            setTimeout(capturarYVerificar, 1600);
        } else {
            mostrarResultado(
                data.mensaje || "No se pudo validar el rostro después de 3 intentos.",
                false
            );

            setTimeout(reiniciarKiosko, 3000);
        }

    } catch (error) {
        activarScanner(false);
        mostrarResultado("Error de conexión con el servidor.", false);
        setTimeout(reiniciarKiosko, 2500);
    }
}

function capturarImagen() {
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;

    const ctx = canvas.getContext("2d");
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    return canvas.toDataURL("image/jpeg", 0.9);
}

function activarScanner(activo) {
    const videoBox = document.querySelector(".video-box");
    if (!videoBox) return;

    if (activo) videoBox.classList.add("escaneando");
    else videoBox.classList.remove("escaneando");
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
    activarScanner(false);

    carnetActual = null;
    intentos = 0;
    procesando = false;
    btnValidar.disabled = false;

    carnetInput.value = "";
    resultado.innerHTML = "";
    resultado.style.display = "none";
    carnetInput.focus();

    cargarSesion();
    reiniciarTemporizador();
}

function reiniciarTemporizador() {
    if (temporizadorReset) clearTimeout(temporizadorReset);

    temporizadorReset = setTimeout(() => {
        if (!procesando) reiniciarKiosko();
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