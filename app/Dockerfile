# Build backend
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

# Runtime
FROM eclipse-temurin:21-jre
RUN useradd --create-home --shell /bin/bash app
WORKDIR /app
COPY --from=build /workspace/target/vistoria-predial-*.jar /app/app.jar
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app
EXPOSE 8080
ENV STORAGE_UPLOAD_DIR=/app/uploads
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
