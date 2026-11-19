# Imagen ANTES: asignador-service solo con el motor Java (sin libmemoria.so).
# Misma estrategia de runtime reducido que la variante nativa, para que la comparacion
# de RQ-03 aisle el costo de la biblioteca.
#   docker build -f deploy/Dockerfile.java -t click/asignador-service:java .

FROM maven:3.9-eclipse-temurin-21 AS jar
WORKDIR /src/asignador-service
COPY asignador-service/pom.xml ./
COPY asignador-service/src ./src
RUN mvn -B -q -Dmaven.test.skip=true package
RUN jlink --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.management,jdk.net \
          --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
          --output /jre-minimo

FROM ubuntu:24.04
RUN useradd --system --uid 10001 --no-create-home appuser
WORKDIR /app
COPY --from=jar /jre-minimo /opt/jre
COPY --from=jar /src/asignador-service/target/asignador-service-0.1.0.jar /app/app.jar
ENV PATH="/opt/jre/bin:${PATH}" \
    ASIGNADOR_MOTOR=java
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
