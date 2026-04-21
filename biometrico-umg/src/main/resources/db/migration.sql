
-- 1. Tabla de carreras disponibles
CREATE TABLE IF NOT EXISTS carreras (
    id      BIGSERIAL    PRIMARY KEY,
    nombre  VARCHAR(200) NOT NULL UNIQUE,
    activo  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 2. Columna carrera en cursos (para filtrar cursos por carrera)
ALTER TABLE cursos ADD COLUMN IF NOT EXISTS carrera VARCHAR(200);

-- 3. Seed inicial con carreras de ejemplo (ajustar según necesidad)
INSERT INTO carreras (nombre) VALUES
    ('Ingeniería en Sistemas de la Información'),
    ('Estadística'),
    ('Psicología')
ON CONFLICT (nombre) DO NOTHING;

-- ── Instalaciones: dejar solo Edificio General y Edificio Mecánica ──────────

-- 4a. Renombrar "UMG Sede La Florida" → "Edificio General"
UPDATE instalaciones
SET nombre = 'Edificio General'
WHERE nombre = 'UMG Sede La Florida';

-- 4b. Crear "Edificio Mecánica" si no existe
INSERT INTO instalaciones (nombre)
SELECT 'Edificio Mecánica'
WHERE NOT EXISTS (SELECT 1 FROM instalaciones WHERE nombre = 'Edificio Mecánica');

-- 4c. Eliminar puertas de instalaciones que NO sean las dos correctas
DELETE FROM puertas
WHERE instalacion_id IN (
    SELECT id FROM instalaciones
    WHERE nombre NOT IN ('Edificio General', 'Edificio Mecánica')
);

-- 4d. Eliminar instalaciones que no sean las dos correctas
DELETE FROM instalaciones
WHERE nombre NOT IN ('Edificio General', 'Edificio Mecánica');

-- ── Nueva tabla: asignación catedrático por sección de curso ────────────────

-- 5. Tabla curso_seccion_asignacion
CREATE TABLE IF NOT EXISTS curso_seccion_asignacion (
    id              BIGSERIAL PRIMARY KEY,
    curso_id        BIGINT       NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    seccion         VARCHAR(1)   NOT NULL,
    catedratico_id  BIGINT       NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    UNIQUE (curso_id, seccion)
);
