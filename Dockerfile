# ============================================
# ETAPA 1: BUILD (compilar con Maven)
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copiar solo el pom.xml primero (mejor cache de capas)
COPY pom.xml .

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar y empaquetar (sin ejecutar tests, se hacen en otro stage)
RUN mvn clean package -DskipTests -B

# ============================================
# ETAPA 2: RUNTIME (imagen ligera con JRE)
# ============================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el JAR generado desde la etapa de build
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto de Spring Boot
EXPOSE 8080

# Health check para Docker Compose
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]