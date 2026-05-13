package com.umg.biometrico.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/camara")
@Slf4j
public class CamaraProxyController {

    private static final String DROIDCAM_URL = "http://192.168.1.87:4747/video";

    private final AtomicReference<byte[]> ultimoFrame = new AtomicReference<>(null);
    private volatile boolean conectado = false;

    @PostConstruct
    public void iniciar() {
        Thread hilo = new Thread(() -> {
            boolean ultimoEstado = false;

            while (true) {
                try {

                    if (!ultimoEstado) {
                    }

                    URL url = new URL(DROIDCAM_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(0); // sin timeout para stream continuo
                    conn.connect();

                    conectado = true;
                    log.info("✅ DroidCam conectado");

                    DataInputStream dis = new DataInputStream(
                            new BufferedInputStream(conn.getInputStream(), 65536));

                    ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
                    boolean dentroDeJpeg = false;
                    int prev = -1;

                    while (true) {
                        int curr = dis.read();
                        if (curr == -1) break;

                        if (!dentroDeJpeg) {
                            // Buscar inicio JPEG: FF D8
                            if (prev == 0xFF && curr == 0xD8) {
                                dentroDeJpeg = true;
                                frameBuffer.reset();
                                frameBuffer.write(0xFF);
                                frameBuffer.write(0xD8);
                            }
                            prev = curr;
                        } else {
                            frameBuffer.write(curr);
                            // Detectar fin JPEG: FF D9
                            if (prev == 0xFF && curr == 0xD9) {
                                // Frame completo
                                byte[] frame = frameBuffer.toByteArray();
                                if (frame.length > 1000) { // ignorar frames corruptos
                                    ultimoFrame.set(frame);
                                }
                                frameBuffer.reset();
                                dentroDeJpeg = false;
                            }
                            prev = curr;
                        }
                    }

                    conn.disconnect();
                    conectado = false;
                    ultimoEstado = false;
                    log.warn("DroidCam stream terminó");

                } catch (Exception e) {
                if (conectado) {
                    log.warn("DroidCam desconectado: {}", e.getMessage());
                    conectado = false;
                }
                ultimoEstado = false;
            }
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        });
        hilo.setDaemon(true);
        hilo.setName("droidcam-reader");
        hilo.start();
    }

    // ─── Stream MJPEG al browser ──────────────────────────────────────────────
    @GetMapping("/stream")
    public void stream(HttpServletResponse response) throws IOException {
        response.setContentType("multipart/x-mixed-replace; boundary=--jpgboundary");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Connection", "keep-alive");

        OutputStream out = response.getOutputStream();

        try {
            while (true) {
                byte[] frame = ultimoFrame.get();
                if (frame != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("----jpgboundary\r\n");
                    sb.append("Content-Type: image/jpeg\r\n");
                    sb.append("Content-Length: ").append(frame.length).append("\r\n\r\n");
                    out.write(sb.toString().getBytes());
                    out.write(frame);
                    out.write("\r\n".getBytes());
                    out.flush();
                }
                Thread.sleep(66); // ~15 fps
            }
        } catch (Exception ignored) {}
    }

    // ─── Foto estática del último frame ──────────────────────────────────────
    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto() {
        byte[] frame = ultimoFrame.get();
        if (frame == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header("Cache-Control", "no-cache")
                .header("Access-Control-Allow-Origin", "*")
                .body(frame);
    }

    // ─── Estado del servicio ──────────────────────────────────────────────────
    @GetMapping("/estado")
    public ResponseEntity<java.util.Map<String, Object>> estado() {
        return ResponseEntity.ok(java.util.Map.of(
                "conectado", conectado,
                "tieneFrame", ultimoFrame.get() != null
        ));
    }
}