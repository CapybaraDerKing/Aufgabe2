# Basis-Image mit JDK und Maven
FROM maven:3.8.5-openjdk-17-slim AS builder

# Arbeitsverzeichnis festlegen
WORKDIR /app

# Kopiere die pom.xml und den src-Ordner
COPY pom.xml .
COPY src ./src

# Maven-Abhängigkeiten herunterladen und die Anwendung bauen
RUN mvn clean package

# Basis-Image für die Ausführung
FROM openjdk:11-jre-slim

# Arbeitsverzeichnis festlegen
WORKDIR /app

# Kopiere das erstellte JAR-File vom Builder-Image
COPY --from=builder /app/target/*.jar app.jar

# Anwendung starten
CMD ["java", "-jar", "app.jar"]

