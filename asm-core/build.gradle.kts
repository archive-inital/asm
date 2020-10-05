description = "ASM Core"

publishing {
    publications {
        create<MavenPublication>("asm-core") {
            groupId = "org.spectral"
            artifactId = "asm-core"
            version = Project.version
            from(components["java"])
        }
    }
}