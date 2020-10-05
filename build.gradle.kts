import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Project.kotlinVersion
    `maven-publish`
}

tasks.withType<Wrapper> {
    gradleVersion = Project.gradleVersion
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.gradle.maven-publish")

    group = "org.spectral"
    version = Project.version

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        asm()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = Project.jvmVersion
    }

    val sourcesJar by tasks.registering(Jar::class) {
        classifier = "sources"
        from(sourceSets.main.get().allSource)
    }

    publishing {
        repositories {
            maven {
                url = uri("https://maven.pkg.github.com/spectral-powered/asm")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_TOKEN")
                }
            }
        }
    }
}