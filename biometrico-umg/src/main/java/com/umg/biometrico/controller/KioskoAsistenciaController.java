package com.umg.biometrico.controller;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.model.SesionAsistencia;
import com.umg.biometrico.repository.PersonaRepository;
import com.umg.biometrico.service.FacialApiService;
import com.umg.biometrico.service.RegistroIngresoService;
import com.umg.biometrico.service.SesionAsistenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kiosko/api")
@RequiredArgsConstructor
public class KioskoAsistenciaController {

    private final SesionAsistenciaService sesionAsistenciaService;
    private final PersonaRepository personaRepository;
    private final FacialApiService facialApiService;
    private final RegistroIngresoService registroIngresoService;

    @GetMapping("/sesion-activa/{camaraId}")
    public ResponseEntity<Map<String, Object>> sesionActiva(@PathVariable Long camaraId) {
        try {
            SesionAsistencia sesion = sesionAsistenciaService.obtenerSesionActivaPorCamara(camaraId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sesionId", sesion.getId(),
                    "cursoId", sesion.getCurso().getId(),
                    "curso", sesion.getCurso().getNombre(),
                    "catedratico", sesion.getCatedratico().getNombreCompleto(),
                    "puertaId", sesion.getPuerta().getId(),
                    "puerta", sesion.getPuerta().getNombre(),
                    "camaraId", sesion.getCamara().getId(),
                    "camara", sesion.getCamara().getNombre()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    @PostMapping("/verificar-asistencia")
    public ResponseEntity<Map<String, Object>> verificarAsistencia(@RequestBody Map<String, Object> body) {
        try {
            Long camaraId = Long.parseLong(body.get("camaraId").toString());
            String carnet = body.get("carnet").toString();
            String imagenBase64 = body.get("imagen").toString();

            SesionAsistencia sesion = sesionAsistenciaService.obtenerSesionActivaPorCamara(camaraId);

            Persona estudiante = personaRepository.findByNumeroCarnet(carnet)
                    .orElseThrow(() -> new RuntimeException("No se encontró estudiante con ese carnet."));

            if (Boolean.TRUE.equals(estudiante.getRestringido())) {
                throw new RuntimeException("Estudiante restringido: " + estudiante.getMotivoRestriccion());
            }

            if (estudiante.getEncodingFacial() == null || estudiante.getEncodingFacial().isBlank()) {
                throw new RuntimeException("El estudiante no tiene rostro enrolado.");
            }

            List<Double> descriptor = facialApiService.descriptorDesdeJson(estudiante.getEncodingFacial());

            if (descriptor == null || descriptor.isEmpty()) {
                throw new RuntimeException("Descriptor facial inválido.");
            }

            Map<String, Object> resultadoFacial =
                    facialApiService.verificarPersona(imagenBase64, descriptor);

            if (resultadoFacial == null) {
                throw new RuntimeException("No hubo respuesta del servicio facial.");
            }

            boolean coincide = Boolean.TRUE.equals(resultadoFacial.get("coincide"));

            if (!coincide) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "mensaje", "El rostro no coincide con el carnet ingresado.",
                        "confianza", resultadoFacial.getOrDefault("confianza", 0)
                ));
            }

            Double confianza = Double.parseDouble(resultadoFacial.get("confianza").toString());

            registroIngresoService.registrarIngresoAsistenciaKiosko(
                    estudiante.getId(),
                    sesion.getPuerta().getId(),
                    sesion.getCurso().getId(),
                    sesion.getCamara().getId(),
                    sesion.getId(),
                    confianza
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "nombre", estudiante.getNombreCompleto(),
                    "carnet", estudiante.getNumeroCarnet(),
                    "curso", sesion.getCurso().getNombre(),
                    "confianza", confianza
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }
}