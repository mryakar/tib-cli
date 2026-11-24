# Written by tib when the service was created; it is yours from then on.
# Builds the service with its Gradle wrapper, then runs it on the Java 25 runtime as a user
# without root rights, as Gradle installs it: its jars and the script that starts it.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src
RUN sh gradlew installDist --no-daemon

FROM eclipse-temurin:25-jre
RUN useradd --system app
WORKDIR /application
COPY --from=build /build/build/install/corner-service/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["/application/bin/corner-service"]
