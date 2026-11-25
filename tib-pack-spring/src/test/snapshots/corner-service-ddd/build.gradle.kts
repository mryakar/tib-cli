// Written by tib when the service was created; it is yours from then on.
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
  java
  id("org.springframework.boot") version "4.1.2"
}

group = "me.yakar"
version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

repositories { mavenCentral() }

dependencies {
  implementation(platform(SpringBootPlugin.BOM_COORDINATES))
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-restclient")
  testImplementation("org.springframework.boot:spring-boot-resttestclient")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:testcontainers-postgresql")
  testImplementation("com.tngtech.archunit:archunit:1.5.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }
