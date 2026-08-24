plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.yonglun"
version = "0.0.1-SNAPSHOT"
description = "itinerary-assistant"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "2.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("com.h2database:h2")

    // 1. Google GenAI Chat Starter (Provides ChatModel)
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")

    // 2. Embedding Model Dependencies
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai-embedding")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springframework.ai:spring-ai-openai")

    // 3. Vector Store: PgVector Store Core
    implementation("org.springframework.ai:spring-ai-pgvector-store")

    // 4. PDF Document Reader
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")

    // 5. Core Vector Store (Provides VectorStore and SimpleVectorStore)
    implementation("org.springframework.ai:spring-ai-vector-store")

    // 6. The Vector Store Advisor (Provides QuestionAnswerAdvisor)
    implementation("org.springframework.ai:spring-ai-vector-store-advisor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
