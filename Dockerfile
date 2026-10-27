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

RUN apk add --no-cache tzdata wget \
    && cp /usr/share/zoneinfo/UTC /etc/localtime \
    && echo "UTC" > /etc/timezone

COPY --from=builder /app/target/kiert-backend.jar app.jar

EXPOSE ${PORT:-8080}

# ============================================================
# ❌ SIN HEALTHCHECK — Render ya hace su propio port scan.
# Si tenías HEALTHCHECK con start-period=60s, Render mataba
# la app porque tardaba más de 60s en arrancar.
# ============================================================
# HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=5 \
#   CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "\
  exec java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:TieredStopAtLevel=1 \
    -Xss256k \
    -Dserver.address=0.0.0.0 \
    -Dserver.port=${PORT:-8080} \
    -Djava.security.egd=file:/dev/./urandom \
    -jar app.jar"]