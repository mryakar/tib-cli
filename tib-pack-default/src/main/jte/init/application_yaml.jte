@import me.yakar.tib.generate.Project
@param Project project
!{String database = project.name().replace('-', '_');}
# Written by tib when the service was created; it is yours from then on.
spring:
  application:
    name: ${project.name()}
  datasource:
    url: jdbc:postgresql://localhost:5432/${database}
    username: ${database}
    password: ${database}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  data:
    web:
      pageable:
        serialization-mode: via-dto
  mvc:
    problemdetails:
      enabled: true
