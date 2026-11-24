@import me.yakar.tib.generate.Project
@param Project project
# Builds the service with its Gradle wrapper, then runs it on the Java ${project.java()} runtime as a user
# without root rights, as Gradle installs it: its jars and the script that starts it.
FROM eclipse-temurin:${project.java()}-jdk AS build
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src
RUN sh gradlew installDist --no-daemon

FROM eclipse-temurin:${project.java()}-jre
RUN useradd --system app
WORKDIR /application
COPY --from=build /build/build/install/${project.name()}/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["/application/bin/${project.name()}"]
