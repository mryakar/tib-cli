// Written by tib when the service was created; it is yours from then on.
plugins {
  java
  application
}

group = "me.yakar"
version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

application { mainClass = "me.yakar.cornerservice.CornerServiceApplication" }

repositories { mavenCentral() }

dependencies {
  implementation("tools.jackson.core:jackson-databind:3.3.0")
  implementation("org.postgresql:postgresql:42.7.14")
  implementation("org.flywaydb:flyway-core:13.12.1")
  runtimeOnly("org.flywaydb:flyway-database-postgresql:13.12.1")
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.mockito:mockito-junit-jupiter:5.24.0")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
  testImplementation("com.tngtech.archunit:archunit:1.5.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }

// Runs the service against PostgreSQL in a container, as the tests do: ./gradlew testRun.
tasks.register<JavaExec>("testRun") {
  group = "application"
  description = "Runs the service against PostgreSQL in a container."
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass = "me.yakar.cornerservice.TestCornerServiceApplication"
}
