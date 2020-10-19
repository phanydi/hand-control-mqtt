
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.ofSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()

plugins {
    idea
    application
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.8"
    id("org.springframework.boot") version "2.3.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
}



buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }

}

repositories {
    google()
    mavenCentral()
    jcenter()
}

application {
    mainClassName = "example.ExampleServer"
}

//defaultTasks = listOf("run")

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation ("org.springframework.boot:spring-boot-starter-web-services")
    implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation ("org.springframework.boot:spring-boot-starter-mustache")

    implementation("io.grpc:grpc-netty-shaded:1.20.0")
    implementation("io.grpc:grpc-protobuf:1.20.0")
    implementation("io.grpc:grpc-stub:1.20.0")
    implementation("io.grpc:grpc-services:1.20.0")
    implementation("io.github.lognet:grpc-spring-boot-starter:4.1.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
}


// compile proto and gRPC
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.7.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.20.0"
        }

    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") {}
            }
        }
    }
}
