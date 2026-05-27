package com.umg.biometrico.controller;

import com.umg.biometrico.service.EmailService;
import com.umg.biometrico.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    @GetMapping("/olvide-password")
    public String mostrarFormularioOlvidePassword() {
        return "auth/olvide-password";
    }

    @PostMapping("/olvide-password")
    public String procesarSolicitudPassword(
            @RequestParam String correo,
            Model model
    ) {

        String token = passwordResetService.generarToken(correo);

        if (token == null) {

            model.addAttribute(
                    "error",
                    "No existe una cuenta asociada a ese correo."
            );

            return "auth/olvide-password";
        }

        String enlace = "https://umg1.duckdns.org/auth/restablecer-password?token=" + token;

        String contenido = """
        <div style="font-family:Arial,sans-serif;background:#f4f7fb;padding:30px;">
          <div style="max-width:650px;margin:auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #dce3ec;">

            <div style="background:#003366;padding:24px;text-align:center;border-bottom:4px solid #c00000;">
              <h1 style="color:#ffffff;margin:0;font-size:26px;">UMG</h1>
              <p style="color:#dbe8ff;margin:4px 0 0;font-size:14px;">
                Universidad Mariano Gálvez
              </p>
            </div>

            <div style="padding:34px 46px;color:#1d3557;">
              <h2 style="text-align:center;color:#003366;margin:0;font-size:26px;">
                Recuperación de contraseña
              </h2>

              <div style="width:70px;height:3px;background:#c00000;margin:18px auto 30px;"></div>

              <p style="font-size:15px;">Hola,</p>

              <p style="font-size:15px;line-height:1.6;">
                Hemos recibido una solicitud para restablecer la contraseña de tu cuenta
                en el sistema de la <strong>Universidad Mariano Gálvez</strong>.
              </p>

              <p style="font-size:15px;line-height:1.6;">
                Si realizaste esta solicitud, haz clic en el siguiente botón para continuar:
              </p>

              <div style="text-align:center;margin:32px 0;">
                <a href="%s"
                   style="background:#003b7a;color:#ffffff;text-decoration:none;
                          padding:14px 36px;border-radius:8px;font-size:16px;
                          font-weight:bold;display:inline-block;">
                  Restablecer mi contraseña
                </a>
              </div>

              <div style="background:#f1f5fb;border-radius:10px;padding:18px 22px;margin-top:20px;">
                <p style="margin:0;font-size:15px;">
                  <strong>Este enlace es válido por 15 minutos.</strong>
                </p>
                <p style="margin:8px 0 0;font-size:14px;line-height:1.5;">
                  Si no solicitaste restablecer tu contraseña, puedes ignorar este mensaje.
                  Tu cuenta permanecerá segura.
                </p>
              </div>

              <hr style="border:none;border-top:1px solid #dce3ec;margin:28px 0;">

              <p style="font-size:13px;color:#5f6f82;line-height:1.5;">
                ¿Problemas para acceder al botón?<br>
                Copia y pega el siguiente enlace en tu navegador:
              </p>

              <p style="font-size:13px;word-break:break-all;">
                <a href="%s" style="color:#0056b3;">%s</a>
              </p>
            </div>

            <div style="background:#f1f5fb;text-align:center;padding:18px;color:#5f6f82;font-size:13px;">
              <p style="margin:0 0 6px;">
                <strong>Por tu seguridad, no compartas este enlace con nadie.</strong>
              </p>
              <p style="margin:0;">
                © 2026 Universidad Mariano Gálvez. Todos los derechos reservados.
              </p>
            </div>

          </div>
        </div>
        """.formatted(enlace, enlace, enlace);

        try {

            emailService.enviarCorreo(
                    correo,
                    "Recuperación de contraseña",
                    contenido
            );

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "No se pudo enviar el correo de recuperación."
            );

            return "auth/olvide-password";
        }

        model.addAttribute(
                "success",
                "Se ha enviado un enlace de recuperación a tu correo."
        );

        return "auth/olvide-password";
    }

    @GetMapping("/restablecer-password")
    public String mostrarFormularioRestablecer(
            @RequestParam String token,
            Model model
    ) {

        boolean valido = passwordResetService.tokenValido(token);

        if (!valido) {

            model.addAttribute(
                    "error",
                    "El enlace es inválido o ha expirado."
            );

            return "auth/error-token";
        }

        model.addAttribute("token", token);

        return "auth/restablecer-password";
    }

    @PostMapping("/restablecer-password")
    public String restablecerPassword(
            @RequestParam String token,
            @RequestParam String password,
            Model model
    ) {

        boolean cambiado = passwordResetService.cambiarPassword(
                token,
                password
        );

        if (!cambiado) {

            model.addAttribute(
                    "error",
                    "No se pudo cambiar la contraseña."
            );

            return "auth/restablecer-password";
        }

        model.addAttribute(
                "success",
                "Contraseña actualizada correctamente."
        );

        return "auth/login";
    }
}