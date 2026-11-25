# Written by tib when the service was created; it is yours from then on.
# Builds the service with its Gradle wrapper, then runs it on the Java 25 runtime as a user
# without root rights, the layers of its jar as Spring Boot extracts them.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src
RUN sh gradlew bootJar --no-daemon \
  && cp build/libs/*.jar application.jar \
  && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --system app
WORKDIR /application
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
