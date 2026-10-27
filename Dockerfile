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

HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE ${PORT:-8080}

# ============================================================
# ENTRYPOINT con flags para ARRANCAR MÁS RÁPIDO
# ============================================================
#  -XX:TieredStopAtLevel=1     → JIT menos agresivo, arranque más rápido
#  -Xss256k                    → Hilos más pequeños
#  -XX:MaxRAMPercentage=75.0   → Usa bien la RAM del contenedor
#  -Dserver.address=0.0.0.0    → Render detecta el puerto
#  -Dserver.port=${PORT}       → Render inyecta PORT (10000)
# ============================================================
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