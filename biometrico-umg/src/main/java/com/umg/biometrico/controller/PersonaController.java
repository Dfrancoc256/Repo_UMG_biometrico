package com.umg.biometrico.controller;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.service.EmailService;
import com.umg.biometrico.service.PdfService;
import com.umg.biometrico.service.PersonaService;
import com.umg.biometrico.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;
    private final PdfService pdfService;
    private final EmailService emailService;
<<<<<<< HEAD
=======
    private final WhatsAppService whatsAppService;
>>>>>>> firmaycursos

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
        return "personas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Persona persona,
                          @RequestParam(value = "foto", required = false) MultipartFile foto,
                          RedirectAttributes redirectAttributes) {
        try {
            boolean esNueva = (persona.getId() == null);
            Persona guardada = personaService.guardar(persona, foto);

            if (esNueva) {
                // Envío asíncrono: no bloquea la respuesta al usuario
                emailService.enviarCarnetPorCorreo(guardada);
                whatsAppService.enviarCarnetPorWhatsApp(guardada);
                redirectAttributes.addFlashAttribute("success",
                    "Persona enrolada correctamente. Se ha enviado el carnet por correo" +
                    (guardada.getTelefono() != null ? " y WhatsApp." : "."));
            } else {
                redirectAttributes.addFlashAttribute("success", "Persona actualizada correctamente.");
            }
        } catch (IllegalArgumentException e) {
            // Errores de validación claros (ej: carnet duplicado)
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
    public ResponseEntity<byte[]> descargarCarnet(@PathVariable Long id) {
        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada con id: " + id));

            byte[] pdf = pdfService.generarCarnetPersona(persona);

            String nombreArchivo = "carnet_" +
                    (persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : persona.getId()) +
                    ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
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

            // Generar PDF
            byte[] pdf = pdfService.generarCarnetPersona(persona);

            String nombreArchivo = "carnet_" +
                    (persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : persona.getId()) +
                    ".pdf";

            // Enviar correo
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
                    (persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : persona.getId()) +
                    ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Autowired
    private WhatsAppService whatsAppService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @GetMapping("/{id}/enviar-whatsapp")
    public String enviarWhatsApp(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

            if (persona.getTelefono() == null || persona.getTelefono().isBlank()) {
                throw new RuntimeException("La persona no tiene teléfono registrado");
            }

            String mediaUrl = appBaseUrl + "/personas/" + id + "/carnet-publico";

            String sid = whatsAppService.enviarCarnetPdf(
                    persona.getTelefono(),
                    mediaUrl
            );

            redirectAttributes.addFlashAttribute("success",
                    "Carnet enviado por WhatsApp ✅ SID: " + sid);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al enviar por WhatsApp ❌: " + e.getMessage());
        }

        return "redirect:/personas/" + id + "/ver";
    }

}