@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.plain.PlainPack
@import me.yakar.tib.spec.TestKind
@param Project project
@param PlainPack pack
plugins {
  java
  application
}

group = "${project.group()}"
version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(${project.java()}) } }

application { mainClass = "${project.basePackage()}.${project.applicationClass()}" }

repositories { mavenCentral() }

dependencies {
  implementation("tools.jackson.core:jackson-databind:${pack.jacksonVersion()}")
  implementation("org.postgresql:postgresql:${pack.pgjdbcVersion()}")
  implementation("org.flywaydb:flyway-core:${pack.flywayVersion()}")
  runtimeOnly("org.flywaydb:flyway-database-postgresql:${pack.flywayVersion()}")
  testImplementation(platform("org.junit:junit-bom:${pack.junitVersion()}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.mockito:mockito-junit-jupiter:${pack.mockitoVersion()}")
  testImplementation("org.testcontainers:testcontainers-postgresql:${pack.testcontainersVersion()}")
@if(project.tests(TestKind.ARCHUNIT))
${"  "}testImplementation("com.tngtech.archunit:archunit:${pack.archunitVersion()}")
@endif
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }

// Runs the service against PostgreSQL in a container, as the tests do: ./gradlew testRun.
tasks.register<JavaExec>("testRun") {
  group = "application"
  description = "Runs the service against PostgreSQL in a container."
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass = "${project.basePackage()}.Test${project.applicationClass()}"
}
