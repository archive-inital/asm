description = "ASM Analyzer"

dependencies {
    api(project(":asm-core"))
}

publishing {
    publications {
        create<MavenPublication>("asm-analyzer") {
            groupId = "org.spectral"
            artifactId = "asm-analyzer"
            version = Project.version
            from(components["java"])
        }
    }
}