# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 --mount=type=secret,id=cloud_ca --mount=type=secret,id=cloud_maven_settings if [ -f /run/secrets/cloud_ca ]; then keytool -importcert -noprompt -alias cloud-proxy -file /run/secrets/cloud_ca -cacerts -storepass changeit; fi; if [ -f /run/secrets/cloud_maven_settings ]; then mvn -s /run/secrets/cloud_maven_settings -B -DskipTests package; else mvn -B -DskipTests package; fi
FROM eclipse-temurin:21-jre
RUN --mount=type=secret,id=cloud_ca if [ -f /run/secrets/cloud_ca ]; then cp /run/secrets/cloud_ca /usr/local/share/ca-certificates/cloud-proxy.crt; update-ca-certificates; fi; apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/backend-0.1.0-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
