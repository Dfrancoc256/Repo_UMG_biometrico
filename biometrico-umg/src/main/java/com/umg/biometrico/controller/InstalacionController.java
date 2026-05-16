package com.umg.biometrico.controller;

import com.umg.biometrico.model.Camara;
import com.umg.biometrico.model.Instalacion;
import com.umg.biometrico.model.Puerta;
import com.umg.biometrico.repository.CamaraRepository;
import com.umg.biometrico.repository.InstalacionRepository;
import com.umg.biometrico.repository.PuertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/instalaciones")
@RequiredArgsConstructor
public class InstalacionController {

    private final InstalacionRepository instalacionRepository;
    private final PuertaRepository puertaRepository;
    private final CamaraRepository camaraRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("instalacion", new Instalacion());
        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Instalacion instalacion, RedirectAttributes ra) {
        instalacionRepository.save(instalacion);
        ra.addFlashAttribute("success", "Instalación guardada.");
        return "redirect:/instalaciones";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {

        instalacionRepository.findById(id).ifPresent(inst -> {
            List<Puerta> puertas = puertaRepository.findByInstalacionId(id);
            List<Camara> camaras = camaraRepository.findByPuerta_Instalacion_Id(id);

            Map<Long, Camara> camarasPorPuerta = camaras.stream()
                    .filter(c -> c.getPuerta() != null && c.getPuerta().getId() != null)
                    .collect(Collectors.toMap(
                            c -> c.getPuerta().getId(),
                            c -> c,
                            (c1, c2) -> c1
                    ));

            model.addAttribute("instalacion", inst);
            model.addAttribute("puertas", puertas);
            model.addAttribute("camarasPorPuerta", camarasPorPuerta);
        });

        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/detalle";
    }

    @GetMapping("/{id}/puerta/nueva")
    public String nuevaPuerta(@PathVariable Long id, Model model) {
        instalacionRepository.findById(id).ifPresent(inst -> model.addAttribute("instalacion", inst));

        model.addAttribute("puerta", new Puerta());
        model.addAttribute("tieneCamara", false);
        model.addAttribute("activeMenu", "instalaciones");

        return "instalaciones/puerta-formulario";
    }

    @PostMapping("/{id}/puerta/guardar")
    public String guardarPuerta(@PathVariable Long id,
                                @ModelAttribute Puerta puerta,
                                @RequestParam(defaultValue = "false") Boolean tieneCamara,
                                RedirectAttributes ra) {

        instalacionRepository.findById(id).ifPresent(inst -> {
            puerta.setInstalacion(inst);
            Puerta puertaGuardada = puertaRepository.save(puerta);

            if (Boolean.TRUE.equals(tieneCamara)) {
                crearOActualizarCamaraAutomatica(puertaGuardada);
            }
        });

        ra.addFlashAttribute("success", "Acceso/Salón registrado correctamente.");
        return "redirect:/instalaciones/" + id;
    }

    @GetMapping("/{id}/puerta/{puertaId}/editar")
    public String editarPuerta(@PathVariable Long id,
                               @PathVariable Long puertaId,
                               Model model) {

        instalacionRepository.findById(id).ifPresent(inst -> model.addAttribute("instalacion", inst));

        puertaRepository.findById(puertaId).ifPresent(p -> {
            model.addAttribute("puerta", p);

            boolean tieneCamara = camaraRepository.findByPuerta_Id(puertaId)
                    .map(c -> Boolean.TRUE.equals(c.getActiva()))
                    .orElse(false);
                    .stream()
                    .anyMatch(c -> Boolean.TRUE.equals(c.getActiva()));

            model.addAttribute("tieneCamara", tieneCamara);
        });
        @ -150,7 +150,7 @@ public class InstalacionController {
            @PathVariable Long puertaId,
            RedirectAttributes ra) {

                camaraRepository.findByPuerta_Id(puertaId).ifPresent(camara -> {
                    camaraRepository.findByPuerta_Id(puertaId).forEach(camara -> {
                        camara.setActiva(false);
                        camara.setPuerta(null);
                        camaraRepository.save(camara);
                        @ -177,8 +177,9 @@ public class InstalacionController {
                        }

                        private void crearOActualizarCamaraAutomatica(Puerta puerta) {
                            Camara camara = camaraRepository.findByPuerta_Id(puerta.getId())
                                    .orElseGet(Camara::new);
                            List<Camara> camaras = camaraRepository.findByPuerta_Id(puerta.getId());

                            Camara camara = camaras.isEmpty() ? new Camara() : camaras.get(0);

                            camara.setPuerta(puerta);
                            camara.setNombre("Cámara " + puerta.getNombre());
                            @ -187,12 +188,19 @@
                                    camara.setToken(null);

                            camaraRepository.save(camara);

                            for (int i = 1; i < camaras.size(); i++) {
                                Camara duplicada = camaras.get(i);
                                duplicada.setActiva(false);
                                duplicada.setPuerta(null);
                                camaraRepository.save(duplicada);
                            }
                        }

                        private void desactivarCamaraSiExiste(Long puertaId) {
                            camaraRepository.findByPuerta_Id(puertaId).ifPresent(camara -> {
                                camaraRepository.findByPuerta_Id(puertaId).forEach(camara -> {
                                    camara.setActiva(false);
                                    camaraRepository.save(camara);
                                });
                            }
                        }