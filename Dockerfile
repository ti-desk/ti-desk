# ==========================================
# ETAPA 1: Construcción de la aplicación (Build)
# ==========================================
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copiar los archivos de configuración de Maven y las dependencias para aprovechar el caché
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Descargar las dependencias (permite compilar offline en ejecuciones posteriores si no cambian)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente del proyecto
COPY src/ ./src/

# Compilar y empaquetar la aplicación omitiendo las pruebas unitarias (el CI ya las valida en GitHub Actions)
RUN ./mvnw clean package -DskipTests

# ==========================================
# ETAPA 2: Imagen ligera de ejecución (Runtime)
# ==========================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Crear un usuario no privilegiado por seguridad (Buenas prácticas SRE)
RUN useradd -ms /bin/bash tidesk && chown -R tidesk:tidesk /app
USER tidesk

# Copiar el archivo JAR empaquetado desde la etapa de construcción
COPY --from=builder /app/target/ti-desk-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto en el que corre Spring Boot (por defecto 8080)
EXPOSE 8080

# Comando por defecto para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]