# ---- Stage 1: build ---------------------------------------------------------
# Building inside the image means the host only needs Docker, not a JDK.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the POM first so the dependency download layer is cached and only
# re-runs when the dependencies themselves change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Selenium needs a browser and a running server, so the end-to-end tests run in
# the pipeline rather than inside the image build. Unit tests do run here.
RUN mvn -B clean package -DskipITs

# ---- Stage 2: runtime -------------------------------------------------------
# A JRE-only base keeps the final image far smaller than the build image.
FROM eclipse-temurin:17-jre AS runtime

# curl is needed by the HEALTHCHECK below; the JRE image does not ship it.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Never run the application as root.
RUN groupadd --system speaker && useradd --system --gid speaker speaker

WORKDIR /app
COPY --from=build /build/target/speaker-session-manager.war app.war

# The H2 file lives here; mount a volume over it to keep data across containers.
RUN mkdir -p /app/data && chown -R speaker:speaker /app
VOLUME ["/app/data"]

USER speaker
EXPOSE 8080

ENV APP_ENV=docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Lets `docker ps` show health, and gives Jenkins something to poll after a deploy.
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.war"]
