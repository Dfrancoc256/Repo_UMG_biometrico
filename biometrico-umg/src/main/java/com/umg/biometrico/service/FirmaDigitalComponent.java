package com.umg.biometrico.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Genera y mantiene en memoria un certificado auto-firmado RSA 2048
 * para firmar electrónicamente los carnets PDF.
 * El certificado es reconocido estructuralmente por cualquier lector PDF
 * (Adobe Acrobat, PDF-XChange, Foxit, etc.) aunque aparezca como
 * "no confiable" al no estar emitido por una CA pública.
 */
@Component
@Getter
@Slf4j
public class FirmaDigitalComponent {

    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    @PostConstruct
    public void init() {
        try {
            // Registrar BouncyCastle como proveedor JCE
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            // Generar par de claves RSA 2048 bits
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
            kpg.initialize(2048, new SecureRandom());
            KeyPair kp = kpg.generateKeyPair();

            // Datos del emisor del certificado
            X500Name subject = new X500Name(
                "CN=UMG Sistema Biometrico, O=Universidad Mariano Galvez de Guatemala, " +
                "OU=Sede La Florida Zona 19, C=GT"
            );

            Date desde = new Date();
            Date hasta = new Date(desde.getTime() + 10L * 365 * 24 * 60 * 60 * 1000); // 10 años

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(System.currentTimeMillis()),
                desde,
                hasta,
                subject,
                kp.getPublic()
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC")
                .build(kp.getPrivate());

            X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));

            this.privateKey = kp.getPrivate();
            this.certificateChain = new Certificate[]{cert};

            log.info("Firma digital UMG inicializada correctamente. Certificado válido hasta: {}", hasta);

        } catch (Exception e) {
            log.error("Error al inicializar firma digital: {}", e.getMessage(), e);
        }
    }

    public boolean isDisponible() {
        return privateKey != null && certificateChain != null;
    }
}
