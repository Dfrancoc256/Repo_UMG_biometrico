package com.umg.biometrico.dto;

public class FotosPersonaDTO {
    private Long personaId;
    private String fotoRuta;

    public FotosPersonaDTO() {
    }

    public FotosPersonaDTO(Long personaId, String fotoRuta) {
        this.personaId = personaId;
        this.fotoRuta = fotoRuta;
    }

    public Long getPersonaId() {
        return personaId;
    }

    public void setPersonaId(Long personaId) {
        this.personaId = personaId;
    }

    public String getFotoRuta() {
        return fotoRuta;
    }

    public void setFotoRuta(String fotoRuta) {
        this.fotoRuta = fotoRuta;
    }
}