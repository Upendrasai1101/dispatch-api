# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run locally / in CI with `./mvnw test`; the image build only packages.
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home appuser
COPY --from=build /app/target/dispatch-load-balancer.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
