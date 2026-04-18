package com.umg.biometrico.dto;

import java.util.List;

public class PythonReconocimientoRequest {
    private String imagenCapturada;
    private List<FotosPersonaDTO> baseFotos;

    public String getImagenCapturada() {
        return imagenCapturada;
    }

    public void setImagenCapturada(String imagenCapturada) {
        this.imagenCapturada = imagenCapturada;
    }

    public List<FotosPersonaDTO> getBaseFotos() {
        return baseFotos;
    }

    public void setBaseFotos(List<FotosPersonaDTO> baseFotos) {
        this.baseFotos = baseFotos;
    }
}