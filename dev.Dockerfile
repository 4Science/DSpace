# syntax=docker/dockerfile:1.7-labs
ARG JDK_VERSION=11
ARG DOCKER_REGISTRY=docker.io
ARG DSPACE_DIR
ARG LOG4J_CONFIG_FILE

FROM ${DOCKER_REGISTRY}/maven:3.9.9-eclipse-temurin-${JDK_VERSION} AS dev
WORKDIR /dspace-src
EXPOSE 8080 5005
ENTRYPOINT ["mvn", "-f dspace/modules/server-boot/pom.xml" , \
 "spring-boot:run", "-Dspring-boot.run.arguments='--dspace.dir=${DSPACE_DIR} --logging.config=${LOG4J_CONFIG_FILE}'", \
 "-Dspring.boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"]
