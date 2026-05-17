# syntax=docker/dockerfile:1.7
# Plantify-Me — Single-Container Multi-Stage Build
#
# Stage 1: Frontend (SvelteKit) → static assets
# Stage 2: Backend (Spring Boot) build → fat JAR (incl. frontend assets)
# Stage 3: Slim JRE-Alpine runtime

# ─── Stage 1: Frontend ───────────────────────────────────────────────────────
FROM node:20-slim AS frontend-build

WORKDIR /app/frontend

# Build-time env for SvelteKit's $env/static/public — must be present
# BEFORE `npm run build` so the values are baked into the bundle.
ARG PUBLIC_AUTH0_DOMAIN
ARG PUBLIC_AUTH0_CLIENT_ID
ARG PUBLIC_AUTH0_AUDIENCE
ENV PUBLIC_AUTH0_DOMAIN=$PUBLIC_AUTH0_DOMAIN
ENV PUBLIC_AUTH0_CLIENT_ID=$PUBLIC_AUTH0_CLIENT_ID
ENV PUBLIC_AUTH0_AUDIENCE=$PUBLIC_AUTH0_AUDIENCE

# Dependency layer — cached unless package*.json changes
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# Source + build (adapter-static writes to ../src/main/resources/static)
COPY frontend/ ./
RUN mkdir -p ../src/main/resources/static && npm run build


# ─── Stage 2: Backend ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app

# Dependency layer — cached unless pom.xml changes.
# Spring AI 2.0.0-M4 is a Milestone; pom.xml declares spring-milestones repo,
# so dependency:go-offline reaches it.
COPY pom.xml ./
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Source layer — invalidated when source changes
COPY src ./src
COPY lombok.config* ./

# Pull in the freshly built frontend assets so they end up in the JAR
COPY --from=frontend-build /app/src/main/resources/static ./src/main/resources/static

# Build the fat JAR. Tests run in CI, not in the image build.
RUN ./mvnw package -DskipTests -B

# Move the produced JAR to a fixed name to decouple the final stage from
# the Maven version naming.
RUN cp target/plantifyme-*.jar /app/app.jar


# ─── Stage 3: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Non-root user for runtime
RUN addgroup -S plantify && adduser -S -G plantify plantify

WORKDIR /app

COPY --from=backend-build --chown=plantify:plantify /app/app.jar /app/app.jar

USER plantify

EXPOSE 8080

# -XX:MaxRAMPercentage caps heap usage relative to the container limit;
# good practice on platforms like Render that pass cgroup limits but
# default Hotspot allocation can be too aggressive.
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75", \
            "-jar", "/app/app.jar"]
