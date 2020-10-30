FROM adoptopenjdk/openjdk11:alpine-slim AS build
RUN mkdir /appbuild
COPY . /appbuild
WORKDIR /appbuild
RUN ./mvnw clean package
ARG JAR_FILE=target/*.jar
RUN mkdir /application
COPY ${JAR_FILE} application/application.jar
WORKDIR application
RUN java -Djarmode=layertools -jar application.jar extract

FROM adoptopenjdk/openjdk11:alpine-jre
WORKDIR application
COPY --from=build appbuild/application/dependencies/ ./
COPY --from=build appbuild/application/snapshot-dependencies/ ./
COPY --from=build appbuild/application/spring-boot-loader/ ./
COPY --from=build appbuild/application/application/ ./
ENV KEYCLOAK_PATH="https://keycloak.keeptrack.xyz.209.182.238.54.nip.io/auth"
ENTRYPOINT ["java", "-Dkeycloak.auth-server-url=${KEYCLOAK_PATH}", "org.springframework.boot.loader.JarLauncher"]
