package com.umg.biometrico.controller;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.service.PersonaService;
import com.umg.biometrico.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PersonaService personaService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Correo o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Sesión cerrada correctamente.");
        }
        return "auth/login";
    }

    @GetMapping("/validar-carnet")
    public String mostrarValidacion() {
        return "auth/validar";
    }

    @PostMapping("/validar-carnet")
    public String validarCarnet(@RequestParam("archivo") MultipartFile archivo, Model model) {
        if (archivo == null || archivo.isEmpty()) {
            model.addAttribute("mensajeError", "No se seleccionó ningún archivo PDF.");
            return "auth/validar";
        }

        try {
            byte[] pdfBytes = archivo.getBytes();
            PdfReader reader = new PdfReader(pdfBytes);

            boolean tieneSignatura = !reader.getAcroFields().getSignatureNames().isEmpty();

            StringBuilder textoBuilder = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                textoBuilder.append(PdfTextExtractor.getTextFromPage(reader, i)).append("\n");
            }
            reader.close();
            String texto = textoBuilder.toString();

            Pattern carnetPattern = Pattern.compile("Carnet[:\\s]+([A-Z0-9-]+)");
            Matcher carnetMatcher = carnetPattern.matcher(texto);
            String carnetExtraido = carnetMatcher.find() ? carnetMatcher.group(1).trim() : null;

            // Fallback: buscar cualquier patrón UMG-XXXXX en el texto
            if (carnetExtraido == null) {
                Matcher fallback = Pattern.compile("UMG-[A-Z0-9]+").matcher(texto);
                if (fallback.find()) carnetExtraido = fallback.group();
            }

            Pattern codigoPattern = Pattern.compile("COD:\\s*([A-F0-9]{10})");
            Matcher codigoMatcher = codigoPattern.matcher(texto);
            String codigoExtraido = codigoMatcher.find() ? codigoMatcher.group(1) : null;

            String codigoEsperado = carnetExtraido != null
                    ? PdfService.generarCodigoValidacion(carnetExtraido) : null;
            boolean codigoValido = codigoEsperado != null && codigoEsperado.equals(codigoExtraido);

            Optional<Persona> personaOpt = carnetExtraido != null
                    ? personaService.buscarPorCarnet(carnetExtraido)
                    : Optional.empty();

            boolean esValido = tieneSignatura && codigoValido && personaOpt.isPresent();

            model.addAttribute("tieneSignatura", tieneSignatura);
            model.addAttribute("carnetExtraido", carnetExtraido);
            model.addAttribute("codigoExtraido", codigoExtraido);
            model.addAttribute("codigoValido", codigoValido);
            model.addAttribute("personaEncontrada", personaOpt.orElse(null));
            model.addAttribute("esValido", esValido);

        } catch (Exception e) {
            model.addAttribute("mensajeError",
                    "No se pudo procesar el archivo. Asegúrese de subir un PDF de carné UMG válido.");
        }

        return "auth/validar";
    }
}
