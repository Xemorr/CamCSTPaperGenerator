plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version("7.1.2")
}

group = "me.xemor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes().attributes["Main-Class"] = "me.xemor.Main";
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    shadow("org.apache.pdfbox:pdfbox:2.0.27")
    shadow("org.yaml:snakeyaml:2.0")
    shadow("org.jsoup:jsoup:1.15.4")
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
    destinationDirectory.set(file("C:\\Users\\samue\\IdeaProjects\\CamCSTPaperGenerator"));
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}