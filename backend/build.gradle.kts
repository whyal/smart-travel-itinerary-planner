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

    // 1. Google GenAI Chat Starter (Provides ChatModel)
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")

    // 2. Google GenAI Embedding Starter (Provides EmbeddingModel)
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai-embedding")

    // 3. Core Vector Store (Provides VectorStore and SimpleVectorStore)
    implementation("org.springframework.ai:spring-ai-vector-store")

    // 4. The Vector Store Advisor (Provides QuestionAnswerAdvisor)
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
