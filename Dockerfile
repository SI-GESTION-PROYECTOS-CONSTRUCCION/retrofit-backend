# ==========================================
# ETAPA 1: Construcción (Build)
# ==========================================
# Usamos una imagen oficial de Maven con Java 17 (Ajusta si usas Java 21)
FROM maven:3.9-eclipse-temurin-17 AS builder

# Establecemos el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos primero el pom.xml y descargamos las dependencias.
# Esto aprovecha el caché de Docker para no descargar el internet entero cada vez que cambies tu código.
COPY pom.xml .
RUN mvn dependency:go-offline

# Ahora sí, copiamos el código fuente de tu proyecto
COPY src ./src

# Compilamos el proyecto saltando los tests para que sea más rápido
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Producción (Run)
# ==========================================
# Usamos una imagen de Java súper ligera (Alpine) para correr la app
FROM eclipse-temurin:17-jre-jammy

# Directorio de trabajo final
WORKDIR /app

# Copiamos el archivo .jar compilado desde la ETAPA 1
COPY --from=builder /app/target/*.jar app.jar

# Exponemos el puerto donde corre Spring Boot (por defecto 8080)
EXPOSE 8080

# Comando mágico para iniciar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]