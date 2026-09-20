FROM eclipse-temurin:25-jdk
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=/backend/target/EOTM.jar
COPY ${JAR_FILE} EOTM.jar
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "EOTM.jar"]