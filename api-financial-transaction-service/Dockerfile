FROM maven:3.9-eclipse-temurin-26 AS build

COPY pom.xml /exact/
WORKDIR /exact

RUN mvn -B dependency:resolve

COPY src /exact/src
# Tests need Docker (Testcontainers) and run in CI / the IDE, not while building the image.
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:25-jre-alpine

LABEL MANTAINER="Dowglas Maia"
ENV SPRING_LOGGING_LEVEL=INFO
# BE-08: the actuator listens on its own port (health, metrics); the API port serves only the business endpoints.
ENV ACTUATOR_PORT=8090
ENV PORT=8088

RUN rm -f /etc/localtime && ln -s /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime

COPY --from=build /exact/target/*.jar /usr/src/app/exactbank.jar

WORKDIR /usr/src/app

# `-noverify` was removed (deprecated since JDK 13); the container-aware defaults of the JVM size the heap.
ENTRYPOINT java \
           -Dfile.encoding=UTF-8 \
           -Dlogging.level.root=${SPRING_LOGGING_LEVEL} \
           -Dmanagement.server.port=${ACTUATOR_PORT} \
           -jar \
           /usr/src/app/exactbank.jar \
           --server.port=${PORT}

EXPOSE ${PORT} ${ACTUATOR_PORT}
