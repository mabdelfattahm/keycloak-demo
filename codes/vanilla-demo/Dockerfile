FROM adoptopenjdk/openjdk11:alpine-slim AS build
RUN mkdir /appbuild
COPY . /appbuild
WORKDIR /appbuild
RUN ./mvnw clean package

FROM adoptopenjdk/openjdk11:alpine-jre
COPY --from=build /appbuild/target/demo*jar-with-dependencies.jar /app/demo.jar
COPY --from=build /appbuild/resources/ /app/resources/
WORKDIR /app
ENV KEYCLOAK_PATH="https://keycloak.keeptrack.xyz"
CMD ["sh", "-c", "java -D'javax.net.ssl.trustStore=./resources/cacerts' -D'CERT=./resources/temporary.jks' -D'KEYCLOAK_PATH=$KEYCLOAK_PATH' -jar ./demo.jar"]

