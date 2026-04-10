# Sistema Biométrico UMG 2.0 — Registro de Ingreso a Instalaciones

**Universidad Mariano Gálvez de Guatemala — Sede La Florida, Zona 19**  
**Programación III — Ingeniería en Sistemas — 2026**

---

## Requisitos del Sistema

| Requisito | Versión |
|---|---|
| Java | 24 (JDK 24) |
| Maven | 3.9+ |
| Spring Boot | 3.3.4 |
| PostgreSQL (servidor remoto) | 15+ |
| IDE recomendado | IntelliJ IDEA / Eclipse / VS Code |

---

## Clonar e Importar en IDE

```bash
git clone https://github.com/TU_USUARIO/biometrico-umg.git
cd biometrico-umg
mvn clean install -DskipTests
```

### IntelliJ IDEA
1. **File → Open** → seleccionar la carpeta `biometrico-umg`
2. IntelliJ detectará automáticamente el `pom.xml`
3. Esperar que Maven descargue las dependencias
4. Ejecutar `BiometricoApplication.java`

### Eclipse / Spring Tool Suite
1. **File → Import → Maven → Existing Maven Projects**
2. Seleccionar la carpeta del proyecto
3. Hacer clic en **Finish**

---

## Configuración

La base de datos está preconfigurada en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://46.62.223.162:5432/biometrico_umg
spring.datasource.username=biometrico_umg_user
spring.datasource.password=Biometrico2026!
```

**No se requieren variables de entorno ni configuración adicional.**

---

## Ejecutar el Proyecto

```bash
# Compilar y ejecutar
mvn spring-boot:run

# O compilar el JAR y ejecutar
mvn clean package -DskipTests
java -jar target/biometrico-umg-2.0.0.jar
```

Acceder en el navegador: **http://localhost:8080**

---

## Estructura del Proyecto (MVC)

```
src/main/java/com/umg/biometrico/
├── BiometricoApplication.java        ← Punto de entrada
├── config/
│   ├── SecurityConfig.java           ← Spring Security (login)
│   └── WebConfig.java                ← Recursos estáticos / uploads
├── controller/                       ← Capa CONTROLADOR (MVC)
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── PersonaController.java
│   ├── CursoController.java
│   ├── AsistenciaController.java
│   ├── InstalacionController.java
│   ├── IngresoController.java
│   ├── ReporteController.java
│   └── HomeController.java
├── service/                          ← Lógica de negocio
│   ├── PersonaService.java
│   ├── CursoService.java
│   ├── AsistenciaService.java
│   ├── RegistroIngresoService.java
│   ├── DashboardService.java
│   └── PdfService.java               ← Generación PDF (iText5 + QR)
├── repository/                       ← Capa MODELO - Repositorios JPA
│   ├── PersonaRepository.java
│   ├── CursoRepository.java
│   ├── AsistenciaRepository.java
│   ├── InstalacionRepository.java
│   ├── PuertaRepository.java
│   ├── RegistroIngresoRepository.java
│   └── CursoEstudianteRepository.java
├── model/                            ← Entidades JPA (tablas BD)
│   ├── Persona.java
│   ├── Curso.java
│   ├── CursoEstudiante.java
│   ├── Asistencia.java
│   ├── Instalacion.java
│   ├── Puerta.java
│   └── RegistroIngreso.java
└── dto/                              ← Data Transfer Objects
    ├── DashboardDTO.java
    └── AsistenciaDTO.java

src/main/resources/
├── application.properties            ← Config BD y app
├── templates/                        ← Vistas Thymeleaf (HTML/CSS)
│   ├── layout/main.html              ← Layout con sidebar
│   ├── auth/login.html
│   ├── dashboard/index.html
│   ├── personas/{lista,formulario,detalle,restringidos}.html
│   ├── cursos/{lista,formulario,detalle}.html
│   ├── asistencia/{cursos,arbol}.html
│   ├── instalaciones/{lista,formulario,detalle,puerta-formulario}.html
│   ├── ingreso/formulario.html
│   └── reportes/{index,puerta,salon,historico}.html
└── static/
    ├── css/style.css                 ← Estilos globales completos
    ├── js/main.js                    ← JavaScript (webcam, alerts)
    └── img/logo-umg.png
```

---

## Módulos Implementados

| Módulo | Funcionalidad |
|---|---|
| **Dashboard** | Estadísticas en tiempo real (personas, ingresos, asistencias, restricciones) |
| **Proceso 1 – Enrolamiento** | Registro biográfico + foto por webcam + generación de carnet PDF con QR |
| **Proceso 2/3 – Ingresos** | Registro de ingresos por puerta principal y salones |
| **Proceso 4 – Asistencia** | Árbol de asistencia por curso (verde=presente, rojo=ausente) + confirmación + PDF |
| **Proceso 5 – Reportes** | Árboles jerárquicos, reportes por puerta, por salón, histórico |
| **Restricciones** | Módulo para restringir/levantar acceso de personas |
| **Instalaciones** | Gestión de edificios, puertas y salones |
| **Seguridad** | Spring Security con roles (admin, catedrático, estudiante) |

---

## Tecnologías Utilizadas

- **Spring Boot 3.3.4** — Framework principal
- **Spring MVC** — Patrón MVC
- **Spring Security** — Autenticación y autorización
- **Spring Data JPA + Hibernate** — ORM para PostgreSQL
- **Thymeleaf** — Motor de plantillas HTML
- **iText 5** — Generación de PDF (carnet y reportes)
- **ZXing** — Generación de códigos QR
- **JavaCV/OpenCV** — Reconocimiento facial (integración)
- **Font Awesome 6** — Iconografía
- **Lombok** — Reducción de código boilerplate

---

## Diagrama de Base de Datos

La BD ya existe en el servidor remoto con las tablas:
`personas`, `cursos`, `curso_estudiantes`, `asistencia`, `instalaciones`, `puertas`, `registro_ingreso`
