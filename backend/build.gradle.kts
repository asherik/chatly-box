plugins {
  java
  id("org.springframework.boot") version "4.1.0-RC1"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "com.chatlybox"
version = "0.1.0"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(22)
  }
}

extra["springCloudVersion"] = "2025.1.1"

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
  }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
  implementation("org.liquibase:liquibase-core:5.0.3")
  implementation("co.elastic.clients:elasticsearch-java:9.4.1")
  implementation("org.elasticsearch.client:elasticsearch-rest-client:9.2.2")
  implementation("io.projectreactor:reactor-core")
  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.21.3")
  implementation("software.amazon.awssdk:s3:2.44.12")
  implementation("org.apache.pdfbox:pdfbox:3.0.7")
  implementation("org.apache.poi:poi-ooxml:5.5.1")
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  runtimeOnly("org.postgresql:postgresql")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
