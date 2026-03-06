# ---- ETAPA 1: Compilar con Maven y Java 25 ----
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Maven (se cachea si pom.xml no cambia)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

# Descargar dependencias
RUN ./mvnw dependency:go-offline -B

# Copiar código y compilar
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ---- ETAPA 2: Imagen final solo con el JAR ----
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

ENV ENTORNO=produccion

# Copiar solo el JAR de la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
