package com.umg.biometrico.controller;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.service.EmailService;
import com.umg.biometrico.service.PdfService;
import com.umg.biometrico.service.PersonaService;
import com.umg.biometrico.service.WhatsAppService;
import com.umg.biometrico.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final RolRepository rolRepository;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @GetMapping
    public String listar(@RequestParam(required = false) String busqueda,
                         @RequestParam(required = false) String tipo,
                         Model model) {
        if (busqueda != null && !busqueda.isBlank()) {
            model.addAttribute("personas", personaService.buscar(busqueda));
            model.addAttribute("busqueda", busqueda);
        } else if (tipo != null && !tipo.isBlank()) {
            model.addAttribute("personas", personaService.listarActivas().stream()
                    .filter(p -> tipo.equalsIgnoreCase(p.getTipoPersona()))
                    .toList());
            model.addAttribute("tipoFiltro", tipo);
        } else {
            model.addAttribute("personas", personaService.listarActivas());
        }
        model.addAttribute("activeMenu", "personas");
        return "personas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("persona", new Persona());
        model.addAttribute("activeMenu", "personas");
        model.addAttribute("roles", rolRepository.findAll());
        return "personas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Persona persona,
                          @RequestParam(value = "foto", required = false) MultipartFile foto,
                          @RequestParam(value = "fotoBase64", required = false) String fotoBase64,
                          RedirectAttributes redirectAttributes) {
        try {
            boolean esNueva = (persona.getId() == null);
            Persona guardada = personaService.guardar(persona, foto, fotoBase64);

            if (esNueva) {
                emailService.enviarCarnetPorCorreo(guardada);
                whatsAppService.enviarCarnetPorWhatsApp(guardada);
                if (guardada.getTelefono() != null && !guardada.getTelefono().isBlank()) {
                    try {
                        String mediaUrl = appBaseUrl + "/personas/" + guardada.getId() + "/carnet-publico";
                        String nombreArchivo = "carnet-" + guardada.getNumeroCarnet() + ".pdf";
                        whatsAppService.enviarCarnetPdfUrl(guardada.getTelefono(), mediaUrl, nombreArchivo);
                    } catch (Exception e) {
                        log.warn("No se pudo enviar PDF por WhatsApp: {}", e.getMessage());
                    }
                }
                redirectAttributes.addFlashAttribute("success",
                        "Persona enrolada correctamente. Se ha enviado el carnet por correo" +
                                (guardada.getTelefono() != null ? " y WhatsApp." : "."));
            } else {
                redirectAttributes.addFlashAttribute("success", "Persona actualizada correctamente.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/personas/nuevo";
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.contains("duplicate key") || msg.contains("unique constraint")) {
                redirectAttributes.addFlashAttribute("error",
                        "Ya existe una persona con ese correo o número de carnet. Verifique los datos.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al guardar: " + msg);
            }
            return "redirect:/personas/nuevo";
        }
        return "redirect:/personas";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        personaService.buscarPorId(id).ifPresent(p -> model.addAttribute("persona", p));
        model.addAttribute("activeMenu", "personas");
        model.addAttribute("roles", rolRepository.findAll());
        return "personas/formulario";
    }

    @GetMapping("/{id}/ver")
    public String ver(@PathVariable Long id, Model model) {
        personaService.buscarPorId(id).ifPresent(p -> model.addAttribute("persona", p));
        model.addAttribute("activeMenu", "personas");
        return "personas/detalle";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        personaService.eliminar(id);
        redirectAttributes.addFlashAttribute("success", "Persona desactivada correctamente.");
        return "redirect:/personas";
    }

    @GetMapping("/restringidos")
    public String restringidos(Model model) {
        model.addAttribute("personas", personaService.listarRestringidas());
        model.addAttribute("activeMenu", "restricciones");
        return "personas/restringidos";
    }

    @PostMapping("/{id}/restringir")
    public String restringir(@PathVariable Long id,
                             @RequestParam String motivo,
                             RedirectAttributes redirectAttributes) {
        personaService.restringir(id, motivo);
        redirectAttributes.addFlashAttribute("success", "Restricción aplicada.");
        return "redirect:/personas/restringidos";
    }

    @PostMapping("/{id}/levantar-restriccion")
    public String levantarRestriccion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        personaService.levantarRestriccion(id);
        redirectAttributes.addFlashAttribute("success", "Restricción levantada.");
        return "redirect:/personas/restringidos";
    }

    @GetMapping("/{id}/carnet")
    public ResponseEntity<byte[]> descargarCarnet(@PathVariable Long id, Principal principal) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isEstudiante = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ESTUDIANTE".equals(a.getAuthority()));
        if (isEstudiante && principal != null) {
            Persona current = personaService.buscarPorCorreo(principal.getName()).orElse(null);
            if (current == null || !current.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada con id: " + id));
            byte[] pdf = pdfService.generarCarnetPersona(persona);
            String nombreArchivo = "carnet_" +
                    (persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : persona.getId()) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> qrPersona(@PathVariable Long id) {
        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada"));
            String carnet = persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : "";
            String cod = PdfService.generarCodigoValidacion(carnet);
            String contenido = "https://umg1.duckdns.org/personas/"
                    + id
                    + "/carnet-publico";
            byte[] png = pdfService.generarQR(contenido, 220, 220);
            if (png == null) return ResponseEntity.internalServerError().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, public")
                    .body(png);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/enviar-carnet")
    public String enviarCarnet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada"));
            if (persona.getCorreo() == null || persona.getCorreo().isBlank()) {
                throw new RuntimeException("La persona no tiene correo registrado");
            }
            byte[] pdf = pdfService.generarCarnetPersona(persona);
            String nombreArchivo = "carnet_" +
                    (persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : persona.getId()) + ".pdf";
            emailService.enviarCarnet(persona.getCorreo(), pdf, nombreArchivo);
            redirectAttributes.addFlashAttribute("success", "Carnet enviado correctamente al correo.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar carnet: " + e.getMessage());
        }
        return "redirect:/personas/" + id + "/ver";
    }

    @GetMapping("/{id}/carnet-publico")
    public ResponseEntity<byte[]> carnetPublico(@PathVariable Long id) {
        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada con id: " + id));
            byte[] pdf = pdfService.generarCarnetPersona(persona);
            String nombreArchivo = "carnet_" +
                    (persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : persona.getId()) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/enviar-whatsapp")
    public String enviarWhatsApp(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada"));
            if (persona.getTelefono() == null || persona.getTelefono().isBlank()) {
                throw new RuntimeException("La persona no tiene teléfono registrado");
            }
            byte[] pdfBytes = pdfService.generarCarnetPersona(persona);
            String mediaUrl = appBaseUrl + "/personas/" + id + "/carnet-publico";
            String nombreArchivo = "carnet-" + persona.getNumeroCarnet() + ".pdf";
            whatsAppService.enviarCarnetPdfUrl(persona.getTelefono(), mediaUrl, nombreArchivo);
            redirectAttributes.addFlashAttribute("success", "Carnet PDF enviado por WhatsApp ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al enviar por WhatsApp ❌: " + e.getMessage());
        }
        return "redirect:/personas/" + id + "/ver";
    }

    // ─── Buscar persona por correo o carnet (para ingreso manual y restricciones) ──
    @GetMapping("/buscar-correo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarPorCorreo(@RequestParam String correo) {
        // Try by email first, then by carnet number
        var persona = personaService.buscarPorCorreo(correo)
                .or(() -> personaService.buscarPorCarnet(correo));
        return persona.map(p -> {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("encontrado", true);
                    resp.put("id",      p.getId());
                    resp.put("nombre",  p.getNombreCompleto());
                    resp.put("carnet",  p.getNumeroCarnet());
                    resp.put("tipo",    p.getTipoPersona());
                    resp.put("fotoUrl", p.getFotoRuta() != null ? "/" + p.getFotoRuta() : null);
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> ResponseEntity.ok(Map.of("encontrado", false)));
    }

    @GetMapping("/buscar-carnet")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarPorCarnet(@RequestParam String carnet) {
        return personaService.buscarPorCarnet(carnet)
                .map(p -> {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("encontrado", true);
                    resp.put("id",      p.getId());
                    resp.put("nombre",  p.getNombreCompleto());
                    resp.put("carnet",  p.getNumeroCarnet());
                    resp.put("tipo",    p.getTipoPersona());
                    resp.put("fotoUrl", p.getFotoRuta() != null ? "/" + p.getFotoRuta() : null);
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> ResponseEntity.ok(Map.of("encontrado", false)));
    }
}