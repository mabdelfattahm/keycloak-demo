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
ENTRYPOINT ["sh", "-c", "java -Djavax.net.ssl.trustStore=./resources/cacerts -DCERT=./resources/temporary.jks -DKEYCLOAK_PATH=${KEYCLOAK_PATH} -jar ./demo.jar"]

