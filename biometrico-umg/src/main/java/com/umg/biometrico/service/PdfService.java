package com.umg.biometrico.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.itextpdf.text.pdf.security.*;
import com.umg.biometrico.model.Asistencia;
import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.Persona;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final FirmaDigitalComponent firmaDigital;

    private static final BaseColor UMG_AZUL = new BaseColor(0, 51, 102);
    private static final BaseColor UMG_AZUL_CLARO = new BaseColor(220, 235, 248);
    private static final BaseColor UMG_CELESTE = new BaseColor(190, 225, 245);
    private static final BaseColor UMG_ROJO = new BaseColor(153, 0, 0);
    private static final BaseColor UMG_DORADO = new BaseColor(204, 153, 51);
    private static final BaseColor BLANCO = BaseColor.WHITE;
    private static final BaseColor GRIS_TEXTO = new BaseColor(70, 70, 70);
    private static final BaseColor GRIS_SUAVE = new BaseColor(235, 235, 235);
    private static final BaseColor VERDE_ESTADO = new BaseColor(0, 120, 0);

    public byte[] generarCarnetPersona(Persona persona) throws Exception {
        Rectangle cardSize = new Rectangle(340, 220);
        Document document = new Document(cardSize, 0, 0, 0, 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        PdfContentByte cb = writer.getDirectContent();

        float w = cardSize.getWidth();
        float h = cardSize.getHeight();

        // Fondo general
        cb.setColorFill(UMG_AZUL_CLARO);
        cb.rectangle(0, 0, w, h);
        cb.fill();

        // Panel lateral izquierdo
        cb.setColorFill(UMG_AZUL);
        cb.rectangle(0, 0, 95, h);
        cb.fill();

        // Franja superior decorativa
        cb.setColorFill(UMG_ROJO);
        cb.rectangle(95, h - 26, w - 95, 26);
        cb.fill();

        // Banda clara decorativa
        cb.setColorFill(UMG_CELESTE);
        cb.rectangle(95, 0, w - 95, 38);
        cb.fill();

        // Marca de agua derecha
        cb.saveState();
        PdfGState gs = new PdfGState();
        gs.setFillOpacity(0.10f);
        cb.setGState(gs);
        cb.setColorFill(UMG_AZUL);
        cb.beginText();
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
        cb.setFontAndSize(bf, 52);
        cb.showTextAligned(Element.ALIGN_CENTER, "UMG", 265, 92, 25);
        cb.endText();
        cb.restoreState();

        // Línea decorativa inferior
        cb.setColorFill(UMG_DORADO);
        cb.rectangle(95, 38, w - 95, 3);
        cb.fill();

        // Logo UMG
        try {
            ClassPathResource logoResource = new ClassPathResource("static/img/logo-umg.png");
            try (InputStream is = logoResource.getInputStream()) {
                byte[] logoBytes = is.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(62, 62);
                cb.addImage(logo, logo.getScaledWidth(), 0, 0, logo.getScaledHeight(), 16, 142);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo del carnet: {}", e.getMessage());
        }

        // Textos laterales UMG
        Font fontUMG = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BLANCO);
        Font fontSede = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, BLANCO);

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("UMG", fontUMG), 47, 132, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("La Florida", fontSede), 47, 118, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("Zona 19", fontSede), 47, 108, 0);

        // Foto
        float fotoX = 15;
        float fotoY = 28;
        float fotoW = 65;
        float fotoH = 78;

        cb.setColorFill(BLANCO);
        cb.roundRectangle(fotoX, fotoY, fotoW, fotoH, 8);
        cb.fill();

        cb.setColorStroke(UMG_DORADO);
        cb.setLineWidth(1.2f);
        cb.roundRectangle(fotoX, fotoY, fotoW, fotoH, 8);
        cb.stroke();

        if (persona.getFotoRuta() != null && !persona.getFotoRuta().isBlank()) {
            try {
                Path rutaFoto = Paths.get(persona.getFotoRuta());

                if (!rutaFoto.isAbsolute()) {
                    rutaFoto = Paths.get("").toAbsolutePath().resolve(persona.getFotoRuta()).normalize();
                }

                if (Files.exists(rutaFoto)) {
                    byte[] fotoBytes = Files.readAllBytes(rutaFoto);
                    Image foto = Image.getInstance(fotoBytes);

                    cb.addImage(foto, fotoW, 0, 0, fotoH, fotoX, fotoY);
                } else {
                    log.warn("La foto no existe en la ruta: {}", rutaFoto);
                }
            } catch (Exception e) {
                log.warn("No se pudo cargar la foto de la persona {}: {}", persona.getNumeroCarnet(), e.getMessage());
            }
        } else {
            Font fotoPlaceholder = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, UMG_AZUL);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("FOTO", fotoPlaceholder), 47, 66, 0);
        }

        // Encabezado derecho
        Font fontTitulo = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BLANCO);
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("CARNÉ UNIVERSITARIO", fontTitulo), 220, 201, 0);

        // Tipo de persona
        Font fontTipo = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, UMG_ROJO);
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(valor(persona.getTipoPersona()).toUpperCase(), fontTipo), 220, 178, 0);

        // Nombre ajustable
        String nombreCompleto = valor(persona.getNombreCompleto());
        Font fontNombre;

        if (nombreCompleto.length() > 28) {
            fontNombre = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, UMG_AZUL);
        } else if (nombreCompleto.length() > 20) {
            fontNombre = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, UMG_AZUL);
        } else {
            fontNombre = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, UMG_AZUL);
        }

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(nombreCompleto, fontNombre), 108, 148, 0);

        // Línea divisoria
        cb.setColorStroke(UMG_AZUL);
        cb.setLineWidth(1f);
        cb.moveTo(108, 142);
        cb.lineTo(318, 142);
        cb.stroke();

        // Datos
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, UMG_AZUL);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRIS_TEXTO);
        Font correoFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, GRIS_TEXTO);
        Font carnetFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, UMG_ROJO);

        float startX = 108;
        float startY = 126;
        float salto = 16;

        String estado = Boolean.TRUE.equals(persona.getRestringido()) ? "RESTRINGIDO" : "ACTIVO";
        Font estadoFont = new Font(
                Font.FontFamily.HELVETICA,
                9,
                Font.BOLD,
                Boolean.TRUE.equals(persona.getRestringido()) ? UMG_ROJO : VERDE_ESTADO
        );

        escribirCampo(cb, startX, startY, "Carnet:", valor(persona.getNumeroCarnet()), labelFont, carnetFont);
        escribirCampo(cb, startX, startY - salto, "Correo:", recortarTexto(valor(persona.getCorreo()), 28), labelFont, correoFont);
        escribirCampo(
                cb,
                startX,
                startY - (salto * 2),
                "Carrera:",
                recortarTexto(
                        persona.getCarrera() != null ? persona.getCarrera().getNombre() : "",
                        26
                ),
                labelFont,
                valueFont
        );
        escribirCampo(cb, startX, startY - (salto * 3), "Sección:", valor(persona.getSeccion()), labelFont, valueFont);
        escribirCampo(cb, startX, startY - (salto * 4), "Estado:", estado, labelFont, estadoFont);

        // QR con URL de verificación + código de validación
        String codigoValidacion = generarCodigoValidacion(persona.getNumeroCarnet());
        // QR con URL pública del carnet PDF
        String contenidoQR = "https://umg1.duckdns.org/personas/"
                + persona.getId()
                + "/carnet-publico";

        // Recuadro visual del QR
        float qrBoxX = 248;
        float qrBoxY = 58;
        float qrBoxW = 62;
        float qrBoxH = 62;

        cb.setColorFill(BLANCO);
        cb.roundRectangle(qrBoxX, qrBoxY, qrBoxW, qrBoxH, 6);
        cb.fill();

        cb.setColorStroke(UMG_AZUL);
        cb.setLineWidth(1f);
        cb.roundRectangle(qrBoxX, qrBoxY, qrBoxW, qrBoxH, 6);
        cb.stroke();

        byte[] qrBytes = generarQR(contenidoQR, 220, 220);
        if (qrBytes != null) {
            try {
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleToFit(52, 52);

                float qrX = qrBoxX + (qrBoxW - qrImage.getScaledWidth()) / 2;
                float qrY = qrBoxY + (qrBoxH - qrImage.getScaledHeight()) / 2;

                qrImage.setAbsolutePosition(qrX, qrY);
                cb.addImage(qrImage);

                Font qrFont = new Font(Font.FontFamily.HELVETICA, 6, Font.BOLD, UMG_AZUL);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("COD: " + codigoValidacion, qrFont), qrBoxX + (qrBoxW / 2), 49, 0);
            } catch (Exception e) {
                log.error("Error al insertar la imagen QR en el PDF: {}", e.getMessage(), e);
            }
        } else {
            log.error("El método generarQR devolvió null.");
        }

        // Pie institucional
        Font pieFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, UMG_AZUL);
        Font pieSecundarioFont = new Font(Font.FontFamily.HELVETICA, 6, Font.NORMAL, GRIS_TEXTO);

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase("Universidad Mariano Gálvez de Guatemala", pieFont), 108, 17, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase("Documento institucional generado por sistema biométrico", pieSecundarioFont), 108, 9, 0);

        document.close();
        byte[] pdfSinFirma = baos.toByteArray();

        return firmarPdf(pdfSinFirma, persona);
    }

    /**
     * Firma el PDF con el certificado auto-firmado de UMG.
     */
    private byte[] firmarPdf(byte[] pdfBytes, Persona persona) {
        if (!firmaDigital.isDisponible()) {
            log.warn("Firma digital no disponible, carnet se entrega sin firmar.");
            return pdfBytes;
        }

        try {
            ByteArrayOutputStream signedOut = new ByteArrayOutputStream();
            PdfReader reader = new PdfReader(pdfBytes);

            PdfStamper stamper = PdfStamper.createSignature(reader, signedOut, '\0');

            LocalDateTime ahora = LocalDateTime.now();
            String fechaHoraImpresion = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            appearance.setReason("Carnet emitido oficialmente por UMG — Impreso: " + fechaHoraImpresion);
            appearance.setLocation("Universidad Mariano Gálvez, Sede La Florida, Zona 19, Guatemala");
            appearance.setContact("noreply.umg.biometrico@gmail.com");
            appearance.setCertificationLevel(PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED);

            appearance.setVisibleSignature(new Rectangle(108, 2, 320, 15), 1, "FirmaUMG");
            appearance.setLayer2Text("Firmado digitalmente por UMG · Impreso: " + fechaHoraImpresion);

            Font firmaFont = new Font(Font.FontFamily.HELVETICA, 5, Font.NORMAL, UMG_AZUL);
            appearance.setLayer2Font(firmaFont);

            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new PrivateKeySignature(
                    firmaDigital.getPrivateKey(), "SHA-256", "BC"
            );

            MakeSignature.signDetached(
                    appearance, digest, signature,
                    firmaDigital.getCertificateChain(),
                    null, null, null, 0,
                    MakeSignature.CryptoStandard.CMS
            );

            reader.close();
            log.debug("PDF del carnet firmado digitalmente para: {}", persona.getNumeroCarnet());
            return signedOut.toByteArray();

        } catch (Exception e) {
            log.error("Error al firmar PDF del carnet {}: {}", persona.getNumeroCarnet(), e.getMessage());
            return pdfBytes;
        }
    }

    public byte[] generarReporteAsistenciaPdf(Curso curso, LocalDate fecha, List<Asistencia> asistencias)
            throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, UMG_AZUL);
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, UMG_ROJO);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

        try {
            ClassPathResource logoResource = new ClassPathResource("static/img/logo-umg.png");
            try (InputStream is = logoResource.getInputStream()) {
                byte[] logoBytes = is.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(70, 70);
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo del reporte: {}", e.getMessage());
        }

        Paragraph titulo = new Paragraph("UNIVERSIDAD MARIANO GÁLVEZ DE GUATEMALA", titleFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        Paragraph sede = new Paragraph("Sede La Florida, Zona 19", subtitleFont);
        sede.setAlignment(Element.ALIGN_CENTER);
        document.add(sede);

        Paragraph repTitulo = new Paragraph("REPORTE DE ASISTENCIA", subtitleFont);
        repTitulo.setAlignment(Element.ALIGN_CENTER);
        repTitulo.setSpacingBefore(5);
        document.add(repTitulo);

        document.add(new Paragraph(" "));
        LineSeparator ls = new LineSeparator(1f, 100, UMG_AZUL, Element.ALIGN_CENTER, 0);
        document.add(new Chunk(ls));
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);

        addInfoRow(infoTable, "Curso:", valor(curso.getNombre()), normalFont);
        addInfoRow(infoTable, "Código:", valor(curso.getCodigo()), normalFont);
        addInfoRow(infoTable, "Catedrático:",
                curso.getCatedratico() != null ? valor(curso.getCatedratico().getNombreCompleto()) : "N/A",
                normalFont);
        addInfoRow(infoTable, "Salón:", valor(curso.getSalon()), normalFont);
        addInfoRow(infoTable, "Horario:", valor(curso.getHorario()), normalFont);
        addInfoRow(infoTable, "Fecha:", fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont);

        document.add(infoTable);
        document.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{5, 30, 40, 25});
        tabla.setSpacingBefore(10);

        String[] headers = {"#", "Nombre Completo", "Correo Electrónico", "Estado"};
        for (String hText : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(hText, headerFont));
            cell.setBackgroundColor(UMG_AZUL);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(cell);
        }

        int presente = 0;
        int ausente = 0;

        for (int i = 0; i < asistencias.size(); i++) {
            Asistencia a = asistencias.get(i);
            BaseColor rowColor = (i % 2 == 0) ? BaseColor.WHITE : GRIS_SUAVE;

            addTableCell(tabla, String.valueOf(i + 1), normalFont, rowColor, Element.ALIGN_CENTER);
            addTableCell(tabla, valor(a.getEstudiante().getNombreCompleto()), normalFont, rowColor, Element.ALIGN_LEFT);
            addTableCell(tabla, valor(a.getEstudiante().getCorreo()), normalFont, rowColor, Element.ALIGN_LEFT);

            boolean esPresente = Boolean.TRUE.equals(a.getPresente());
            BaseColor estadoColor = esPresente ? new BaseColor(0, 128, 0) : new BaseColor(200, 0, 0);
            Font estadoFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);

            PdfPCell estadoCell = new PdfPCell(new Phrase(esPresente ? "PRESENTE" : "AUSENTE", estadoFont));
            estadoCell.setBackgroundColor(estadoColor);
            estadoCell.setPadding(5);
            estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(estadoCell);

            if (esPresente) {
                presente++;
            } else {
                ausente++;
            }
        }

        document.add(tabla);
        document.add(new Paragraph(" "));

        Paragraph resumen = new Paragraph(
                "Total Presentes: " + presente +
                        "   |   Total Ausentes: " + ausente +
                        "   |   Total Inscritos: " + asistencias.size(),
                subtitleFont
        );
        resumen.setAlignment(Element.ALIGN_CENTER);
        document.add(resumen);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        PdfPTable firmaTable = new PdfPTable(2);
        firmaTable.setWidthPercentage(80);

        PdfPCell firmaCell1 = new PdfPCell();
        firmaCell1.setBorderWidthTop(1);
        firmaCell1.setBorderWidthBottom(0);
        firmaCell1.setBorderWidthLeft(0);
        firmaCell1.setBorderWidthRight(0);
        firmaCell1.addElement(new Paragraph("Firma del Catedrático", normalFont));
        firmaTable.addCell(firmaCell1);

        PdfPCell firmaCell2 = new PdfPCell();
        firmaCell2.setBorderWidthTop(1);
        firmaCell2.setBorderWidthBottom(0);
        firmaCell2.setBorderWidthLeft(0);
        firmaCell2.setBorderWidthRight(0);
        firmaCell2.addElement(new Paragraph("Firma del Director", normalFont));
        firmaTable.addCell(firmaCell2);

        document.add(firmaTable);

        document.close();
        return baos.toByteArray();
    }

    private void escribirCampo(PdfContentByte cb, float x, float y, String etiqueta, String valor,
                               Font fontEtiqueta, Font fontValor) {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(etiqueta, fontEtiqueta), x, y, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(valor != null ? valor : "", fontValor), x + 42, y, 0);
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font font) {
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    public byte[] generarReporteIngreso(String titulo, String subtitulo,
                                        List<com.umg.biometrico.model.RegistroIngreso> registros)
            throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont    = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD,  UMG_AZUL);
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,  UMG_ROJO);
        Font normalFont   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.BLACK);
        Font headerFont   = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,  BaseColor.WHITE);
        Font smallFont    = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, GRIS_TEXTO);

        try {
            ClassPathResource logoResource = new ClassPathResource("static/img/logo-umg.png");
            try (InputStream is = logoResource.getInputStream()) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.scaleToFit(55, 55);
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo: {}", e.getMessage());
        }

        Paragraph h1 = new Paragraph("UNIVERSIDAD MARIANO GÁLVEZ DE GUATEMALA", titleFont);
        h1.setAlignment(Element.ALIGN_CENTER);
        document.add(h1);

        Paragraph sede = new Paragraph("Sede La Florida, Zona 19", subtitleFont);
        sede.setAlignment(Element.ALIGN_CENTER);
        document.add(sede);

        Paragraph repTit = new Paragraph(titulo.toUpperCase(), subtitleFont);
        repTit.setAlignment(Element.ALIGN_CENTER);
        repTit.setSpacingBefore(4);
        document.add(repTit);

        Paragraph sub = new Paragraph(subtitulo, normalFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(2);
        document.add(sub);

        Paragraph fechaGen = new Paragraph(
                "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                smallFont);
        fechaGen.setAlignment(Element.ALIGN_RIGHT);
        document.add(fechaGen);

        document.add(new Paragraph(" "));
        document.add(new Chunk(new LineSeparator(1f, 100, UMG_AZUL, Element.ALIGN_CENTER, 0)));
        document.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{4, 28, 32, 14, 22});
        tabla.setSpacingBefore(8);

        for (String hText : new String[]{"#", "Nombre Completo", "Correo Electrónico", "Tipo", "Fecha / Hora"}) {
            PdfPCell cell = new PdfPCell(new Phrase(hText, headerFont));
            cell.setBackgroundColor(UMG_AZUL);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(cell);
        }

        for (int i = 0; i < registros.size(); i++) {
            com.umg.biometrico.model.RegistroIngreso r = registros.get(i);
            Persona p = r.getPersona();
            BaseColor rowColor = (i % 2 == 0) ? BaseColor.WHITE : GRIS_SUAVE;

            addTableCell(tabla, String.valueOf(i + 1), normalFont, rowColor, Element.ALIGN_CENTER);
            addTableCell(tabla, p != null ? valor(p.getNombreCompleto()) : "—", normalFont, rowColor, Element.ALIGN_LEFT);
            addTableCell(tabla, p != null ? valor(p.getCorreo()) : "—", smallFont, rowColor, Element.ALIGN_LEFT);
            addTableCell(tabla, p != null ? valor(p.getTipoPersona()) : "—", normalFont, rowColor, Element.ALIGN_CENTER);
            addTableCell(tabla, r.getFechaHora() != null
                    ? r.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "—", smallFont, rowColor, Element.ALIGN_CENTER);
        }

        document.add(tabla);

        Paragraph total = new Paragraph("Total de registros: " + registros.size(), subtitleFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(10);
        document.add(total);

        document.close();
        return baos.toByteArray();
    }

    public byte[] generarQR(String contenido, int ancho, int alto) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            BitMatrix bitMatrix = qrWriter.encode(
                    contenido,
                    BarcodeFormat.QR_CODE,
                    ancho,
                    alto,
                    hints
            );

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);

            byte[] resultado = baos.toByteArray();
            log.info("QR generado correctamente. Tamaño en bytes: {}", resultado.length);
            return resultado;

        } catch (Exception e) {
            log.error("Error al generar el QR: {}", e.getMessage(), e);
            return null;
        }
    }

    private String valor(String texto) {
        return texto != null ? texto : "";
    }

    private String recortarTexto(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo - 3) + "...";
    }

    public static String generarCodigoValidacion(String carnet) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String entrada = (carnet != null ? carnet.trim() : "") + ":UMG-BIOMETRICO-SEDE-Z19";
            byte[] hash = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "N/A";
        }
    }
}