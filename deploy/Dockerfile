# Imagen DESPUES: asignador-service con libmemoria.so (motor nativo por JNA).
# Se construye desde la raiz del repositorio:
#   docker build -f deploy/Dockerfile -t click/asignador-service:nativo .

# ---- Etapa 1: compilar libmemoria.so (gcc solo existe en esta etapa) ----
# Ubuntu 24.04 (glibc 2.39) = la misma base que la imagen final
FROM ubuntu:24.04 AS compilador-c
RUN apt-get update \
 && apt-get install -y --no-install-recommends gcc make libc6-dev \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /src/c
COPY c/memoria.h c/memoria.c c/Makefile ./
RUN make libmemoria.so

# ---- Etapa 2: construir el JAR y un runtime Java reducido (jlink) ----
FROM maven:3.9-eclipse-temurin-21 AS jar
WORKDIR /src/asignador-service
COPY asignador-service/pom.xml ./
COPY asignador-service/src ./src
RUN mvn -B -q -Dmaven.test.skip=true package
RUN jlink --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.management,jdk.net \
          --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
          --output /jre-minimo

# ---- Etapa 3: imagen final (sin gcc, sin Maven, sin JDK completo) ----
FROM ubuntu:24.04
RUN useradd --system --uid 10001 --no-create-home appuser
WORKDIR /app
COPY --from=jar /jre-minimo /opt/jre
COPY --from=compilador-c /src/c/libmemoria.so /app/lib/libmemoria.so
COPY --from=jar /src/asignador-service/target/asignador-service-0.1.0.jar /app/app.jar
ENV PATH="/opt/jre/bin:${PATH}" \
    ASIGNADOR_MOTOR=native
EXPOSE 8080
USER appuser
# JNA busca libmemoria.so en /app/lib (decision D-14)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djna.library.path=/app/lib", "-jar", "/app/app.jar"]
