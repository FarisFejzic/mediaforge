# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the whole multi-module project (parent pom + all modules)
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY api/pom.xml api/pom.xml
COPY worker/pom.xml worker/pom.xml

# Pre-fetch dependencies (cached layer — only re-runs if a pom changes)
RUN mvn -q -e -B dependency:go-offline -pl worker -am || true

# Now copy sources and build the worker (+ its dependency: common)
COPY common/src common/src
COPY worker/src worker/src
RUN mvn -q -B clean package -pl worker -am -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Install FFmpeg (worker shells out to it)
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Copy just the built jar from the build stage
COPY --from=build /build/worker/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]