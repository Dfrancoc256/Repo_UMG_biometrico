package com.umg.biometrico.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursoEstudianteId implements Serializable {
    private Long curso;
    private Long estudiante;
}
