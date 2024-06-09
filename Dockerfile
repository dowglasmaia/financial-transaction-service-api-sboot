FROM maven:3.8.4-openjdk-17-slim AS build

COPY pom.xml /exact/
WORKDIR /exact

RUN mvn dependency:resolve

COPY src /exact/src
RUN mvn clean install


FROM amazoncorretto:17-alpine3.16

LABEL MANTAINER="Dowglas Maia"
ENV SPRING_LOGGING_LEVEL=INFO
ENV ACTUATOR_PORT=8088
ENV PORT=8088

RUN rm -f /etc/localtime && ln -s /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime

COPY --from=build /exact/target/*.jar /usr/src/app/exactbank.jar

WORKDIR /usr/src/app

ENTRYPOINT java \
           -noverify \
           -Dfile.encoding=UTF-8 \
           -Dlogging.level.root=${SPRING_LOGGING_LEVEL} \
           -Dmanagement.server.port=${ACTUATOR_PORT} \
           -jar \
           /usr/src/app/exactbank.jar \
           --server.port=${PORT}

EXPOSE ${PORT} ${ACTUATOR_PORT}