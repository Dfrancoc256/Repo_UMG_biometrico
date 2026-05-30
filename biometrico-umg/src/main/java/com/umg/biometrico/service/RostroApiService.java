package com.umg.biometrico.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RostroApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String segmentarRostro(String imagenBase64) {

        String url = "http://www.server.daossystem.pro:5030/api/Rostro/Segmentar";

        Map<String, String> body = new HashMap<>();
        body.put("RostroA", imagenBase64);
        body.put("RostroB", imagenBase64);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                body,
                String.class
        );

        return response.getBody();
    }
}