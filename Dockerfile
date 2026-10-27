# ============================================
# ETAPA 1: BUILD
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================
# ETAPA 2: RUNTIME
# ============================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Zona horaria UTC + wget para healthcheck
RUN apk add --no-cache tzdata wget \
    && cp /usr/share/zoneinfo/UTC /etc/localtime \
    && echo "UTC" > /etc/timezone

# Usuario no-root (pero arrancamos como root para poder copiar el truststore)
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar el JAR
COPY --from=builder /app/target/kiert-backend.jar app.jar

# Healthcheck usando $PORT (Render lo inyecta)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Exponer $PORT (Render lo inyecta)
EXPOSE ${PORT:-8080}

# Entrypoint:
#   1. Si existe el truststore en /etc/secrets/, lo copia a /app/ y le da permisos
#   2. Configura Java con el truststore
#   3. Fuerza 0.0.0.0 para que Render detecte el puerto
#   4. Arranca la app
ENTRYPOINT ["sh", "-c", "\
  if [ -f /etc/secrets/truststore.jks ]; then \
    cp /etc/secrets/truststore.jks /app/truststore.jks && chmod 644 /app/truststore.jks; \
  fi; \
  exec java \
    -Djavax.net.ssl.trustStore=/app/truststore.jks \
    -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD:-cambiar123} \
    -Dserver.address=0.0.0.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -jar app.jar"]