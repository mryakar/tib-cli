@import me.yakar.tib.generate.Project
@param Project project
# Written by tib when the service was created; it is yours from then on.
spring:
  application:
    name: ${project.name()}
  datasource:
    url: jdbc:postgresql://localhost:5432/${project.name()}
    username: ${project.name()}
    password: ${project.name()}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
