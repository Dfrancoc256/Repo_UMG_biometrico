
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
