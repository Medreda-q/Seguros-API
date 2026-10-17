# SegurosAPI 🛡️

API REST empresarial y segura para la gestión integral de pólizas de seguros (Auto, Hogar y Vida) y tramitación de siniestros en el mercado español.

> **Nota:** *Modelo simplificado con fines pedagógicos y de formación en ciberseguridad / desarrollo seguro backend en Java.*

---

## 🚀 Tecnologías Utilizadas

- **Lenguaje:** Java 17 (OpenJDK Temurin 17)
- **Framework:** Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Jakarta Bean Validation)
- **Base de Datos:** PostgreSQL 16 (con paridad estricta entre entornos `dev` y `test`)
- **Migraciones de Esquema:** Flyway Migrations (modo `ddl-auto=validate`)
- **Documentación de API:** SpringDoc OpenAPI v2 / Swagger UI
- **Pruebas Automatizadas:** JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers
- **Contenerización:** Docker Compose (PostgreSQL)
- **Gestor de Construcción:** Apache Maven 3.9 (con Maven Wrapper `mvnw` / `mvnw.cmd`)

---

## 📁 Estructura del Proyecto

```text
seguros-api/
├── .gitignore                          # Exclusión de target, .env, perfiles locales e IDEs
├── docker-compose.yml                  # Contenedor PostgreSQL 16
├── docker/
│   └── init-databases.sql              # Creación automática de seguros_db y seguros_test_db
├── pom.xml                             # Definición de dependencias y plugins Maven
├── mvnw / mvnw.cmd                     # Maven Wrapper independiente
├── BUGS.md                             # Bitácora de incidencias y análisis de causas raíz
├── STUDY_GUIDE.md                      # Guía de estudio pedagógica (teoría, Q&A y ejercicios)
├── README.md                           # Documentación principal del repositorio
├── docs/
│   └── documento_funcional.md          # Especificación funcional completa en español
└── src/
    ├── main/
    │   ├── java/com/seguros/api/
    │   │   └── SegurosApiApplication.java
    │   └── resources/
    │       ├── application.yml         # Configuración base (ddl-auto=validate)
    │       ├── application-dev.yml     # Perfil dev (PostgreSQL local)
    │       ├── application-test.yml    # Perfil test (PostgreSQL para pruebas)
    │       └── db/migration/
    │           └── V1__initial_schema.sql # Esquema inicial Flyway (clientes, polizas, siniestros)
    └── test/
        └── java/com/seguros/api/
```

---

## ⚙️ Requisitos Previos

- **Java Development Kit (JDK) 17+**
- **Docker** y **Docker Compose**
- **Git**

---

## 🛠️ Instalación y Puesta en Marcha

### 1. Iniciar el servicio de base de datos
Arranca el contenedor de PostgreSQL con las dos bases de datos inicializadas (`seguros_db` y `seguros_test_db`):
```bash
docker compose up -d
```

### 2. Compilar el proyecto y descargar dependencias
Utiliza el Maven Wrapper incluido para compilar sin necesidad de instalar Maven globalmente:
```bash
# En Windows (cmd o PowerShell):
.\mvnw.cmd clean compile

# En Linux o macOS:
./mvnw clean compile
```

### 3. Ejecutar la aplicación en entorno de desarrollo
```bash
# En Windows:
.\mvnw.cmd spring-boot:run

# En Linux o macOS:
./mvnw spring-boot:run
```
La API estará disponible en `http://localhost:8080`.

### 4. Acceso a la documentación interactiva (Swagger UI)
Una vez en ejecución, puedes explorar y probar los endpoints en:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### 5. Ejecución de pruebas automatizadas
Para ejecutar la suite completa de pruebas unitarias y de integración sobre PostgreSQL:
```bash
.\mvnw.cmd test
```

---

## 🔒 Fases del Proyecto

- [x] **Fase 1 (En curso):** Core API Funcional (CRUD Cliente, Póliza, Siniestro, tarificación de primas, transiciones de estado, validador DNI/NIE módulo 23, Flyway, Swagger y tests).
- [ ] **Fase 2:** Capa de Seguridad (Autenticación JWT, contraseñas BCrypt, control de acceso RBAC por roles, mitigación de IDOR, pistas de auditoría y enmascaramiento de datos sensibles).
- [ ] **Fase 3:** Pruebas de Seguridad y Pentesting (Análisis de vulnerabilidades, escaneos locales con Nmap, SQLMap, tampering de JWT y redacción de informe técnico de seguridad).
