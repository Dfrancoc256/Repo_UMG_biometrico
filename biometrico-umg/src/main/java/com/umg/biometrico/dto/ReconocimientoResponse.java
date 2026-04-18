package com.umg.biometrico.dto;

public class ReconocimientoResponse {

    private boolean encontrado;
    private boolean asistenciaRegistrada;
    private Long personaId;
    private String nombre;
    private String mensaje;

    public boolean isEncontrado() {
        return encontrado;
    }

    public void setEncontrado(boolean encontrado) {
        this.encontrado = encontrado;
    }

    public boolean isAsistenciaRegistrada() {
        return asistenciaRegistrada;
    }

    public void setAsistenciaRegistrada(boolean asistenciaRegistrada) {
        this.asistenciaRegistrada = asistenciaRegistrada;
    }

    public Long getPersonaId() {
        return personaId;
    }

    public void setPersonaId(Long personaId) {
        this.personaId = personaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}