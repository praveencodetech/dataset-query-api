# Stage 1: Build the application using Maven and Temurin JDK 17
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Pre-fetch dependencies to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package application (tests run in CI/CD pipeline)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal, secure JRE 17 runtime environment
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Create dedicated non-root application group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the executable jar using the fixed finalName (never matches *.jar.original)
COPY --from=builder --chown=appuser:appgroup /build/target/dataset-service.jar app.jar

# Switch to non-root user
USER appuser

# Expose standard container port
EXPOSE 8080

# JVM memory tuning for Render free tier (512 MB).
# MaxRAMPercentage=70 → ~358 MB heap; SerialGC reduces GC overhead threads.
# Override at runtime by setting JAVA_TOOL_OPTIONS in the Render dashboard.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

# exec-form ENTRYPOINT: JVM receives OS signals (SIGTERM) for graceful shutdown.
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
