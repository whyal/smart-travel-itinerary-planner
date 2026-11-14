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
    implementation("org.postgresql:postgresql")

    // 1. Google GenAI Chat Starter (Provides ChatModel)
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")

    // 2. Embedding Model Dependencies
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai-embedding")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springframework.ai:spring-ai-openai")

    // 3. Vector Store: PgVector Store Core
    implementation("org.springframework.ai:spring-ai-pgvector-store")

    // 4. Persistent chat memory (JDBC)
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc")

    // 5. PDF Document Reader
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")

    // 6. Core Vector Store (Provides VectorStore and SimpleVectorStore)
    implementation("org.springframework.ai:spring-ai-vector-store")

    // 7. The Vector Store Advisor (Provides QuestionAnswerAdvisor)
    implementation("org.springframework.ai:spring-ai-vector-store-advisor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = file(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val cleanLine = if (line.startsWith("export ")) line.removePrefix("export ").trim() else line
                val parts = cleanLine.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val rawVal = parts[1].trim()
                    val valWithoutComment = if (rawVal.contains(" #")) rawVal.substringBefore(" #").trim() else rawVal
                    val value = valWithoutComment.trim('"', '\'')
                    environment(key, value)
                }
            }
        }
    }
}
