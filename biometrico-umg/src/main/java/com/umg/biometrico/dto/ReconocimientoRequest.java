package com.umg.biometrico.dto;

public class ReconocimientoRequest {

    private String imagenBase64;
    private Long instalacionId;
    private Long puertaId;
    private String metodoRegistro;

    public String getImagenBase64() {
        return imagenBase64;
    }

    public void setImagenBase64(String imagenBase64) {
        this.imagenBase64 = imagenBase64;
    }

    public Long getInstalacionId() {
        return instalacionId;
    }

    public void setInstalacionId(Long instalacionId) {
        this.instalacionId = instalacionId;
    }

    public Long getPuertaId() {
        return puertaId;
    }

    public void setPuertaId(Long puertaId) {
        this.puertaId = puertaId;
    }

    public String getMetodoRegistro() {
        return metodoRegistro;
    }

    public void setMetodoRegistro(String metodoRegistro) {
        this.metodoRegistro = metodoRegistro;
    }
}