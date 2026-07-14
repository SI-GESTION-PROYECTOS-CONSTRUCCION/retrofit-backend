# Retrofit Backend API

Este es el proyecto backend del sistema **Retrofit**, diseñado para gestionar operaciones, reportes, usuarios y auditoría de la plataforma. Está construido bajo la arquitectura de microservicios monolíticos utilizando **Java 17** y el ecosistema **Spring Boot**.

## 🚀 Tecnologías Principales

- **Java 17**: Lenguaje principal.
- **Spring Boot**: Framework base para la API REST.
- **Spring Data JPA & Hibernate**: ORM para persistencia y acceso a datos.
- **PostgreSQL**: Base de datos relacional principal.
- **Spring Security & JWT (JSON Web Tokens)**: Gestión de autenticación, autorización basada en roles (RBAC) y control de permisos granulares.
- **AWS SDK (S3)**: Integración para el almacenamiento de archivos y fotos utilizando **DigitalOcean Spaces** (compatible con S3).
- **Thymeleaf & Flying Saucer**: Motor de plantillas HTML y generador de reportes en PDF dinámicos.
- **JSoup**: Procesamiento y sanitización de HTML para reportes.
- **Springdoc OpenAPI (Swagger)**: Documentación automática de la API.
- **Lombok**: Reducción de código boilerplate (getters, setters, builders, etc.).

---

## 📂 Arquitectura y Estructura del Proyecto

El proyecto sigue una arquitectura multicapa (Layered Architecture) para mantener la separación de responsabilidades:

```text
src/main/java/com/retrofit/backend/
├── annotation/      # Anotaciones personalizadas (ej. @AuditChange para auditoría automática)
├── aspect/          # Programación Orientada a Aspectos (AOP), interceptores de auditoría
├── auth/            # Configuración de Spring Security, Filtros JWT, Detalles de Usuario
├── config/          # Configuraciones globales (CORS, S3, OpenAPI, WebMvc)
├── controller/      # Controladores REST (Endpoints expuestos al cliente)
├── dto/             # Data Transfer Objects (Validación de entrada y salida)
├── enums/           # Enumeraciones del dominio
├── exceptions/      # Manejo global de excepciones (@ControllerAdvice)
├── model/           # Entidades JPA (Tablas de base de datos)
├── repository/      # Interfaces de acceso a datos (Spring Data Repositories)
└── service/         # Interfaces de lógica de negocio y su implementación (carpeta `impl`)
```

---

## 🛠 Configuración del Entorno Local

Para ejecutar este proyecto localmente, debes tener instalado **Java 17** y **PostgreSQL**.

### 1. Variables de Entorno / Propiedades
El proyecto utiliza el archivo `src/main/resources/application.properties`. Asegúrate de que las credenciales de tu base de datos y de DigitalOcean Spaces sean correctas.

Ejemplo de `application.properties`:
```properties
server.servlet.context-path=/api/v1

# Configuración de Base de Datos
spring.datasource.url=jdbc:postgresql://localhost:5432/retrofit
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=update

# Seguridad (JWT)
jwt.secret=UnaClaveSuperSeguraDeAlMenos256Bits1234567890
jwt.expiration=3600000

# AWS / DigitalOcean Spaces Config
aws.spaces.endpoint=${SPACES_ENDPOINT}
aws.spaces.region=${SPACES_REGION}
aws.spaces.bucket=${SPACES_BUCKET}
aws.spaces.key=${SPACES_KEY}
aws.spaces.secret=${SPACES_SECRET}
```

> **Nota:** Puedes exportar las variables `SPACES_*` en tu terminal o configurarlas en tu IDE para evitar exponer credenciales directamente en el código.

### 2. Ejecutar el Proyecto
Para iniciar la aplicación, puedes utilizar el comando Maven wrapper o la ejecución nativa de tu IDE (IntelliJ, Eclipse, VSCode).

Con Maven:
```bash
./mvnw spring-boot:run
```

El servidor iniciará por defecto en el puerto `8080`, con el context path configurado en `/api/v1`.

---

## 📖 Documentación de la API (Swagger)

Gracias a `springdoc-openapi`, el proyecto autogenera una interfaz de documentación Swagger en vivo. Una vez que el servidor esté en ejecución, puedes acceder a ella navegando a:

- **Swagger UI:** `http://localhost:8080/api/v1/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8080/api/v1/v3/api-docs`

---

## 🔐 Seguridad y Permisos (RBAC)

El sistema utiliza un control de acceso basado en Roles y Permisos individuales granulares (`USER_CREATE`, `USER_UPDATE`, etc.). 

- La clase `DataInitializer` se encarga de popular la base de datos con los roles por defecto (`ADMIN`, `INGENIERO_RESIDENTE`, `ALMACENERO`) y el usuario administrador inicial, en caso de que la tabla esté vacía.
- Los Endpoints están protegidos a nivel de método utilizando la anotación `@PreAuthorize("hasAuthority('PERMISO_NECESARIO')")`.

## 📜 Auditoría Automática
El backend implementa un sistema de **Auditoría (AOP)** mediante la anotación `@AuditChange`. Esto permite interceptar los métodos críticos del servicio y registrar automáticamente los cambios (estado anterior y nuevo) de cualquier entidad importante en la base de datos sin contaminar la lógica de negocio.
