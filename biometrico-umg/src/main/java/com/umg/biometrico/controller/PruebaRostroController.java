package com.umg.biometrico.controller;

import com.umg.biometrico.service.RostroApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/prueba-rostro")
@RequiredArgsConstructor
public class PruebaRostroController {

    private final RostroApiService rostroApiService;

    @PostMapping("/segmentar")
    @ResponseBody
    public String probarSegmentacion(@RequestParam String imagenBase64) {

        return rostroApiService.segmentarRostro(imagenBase64);
    }
}