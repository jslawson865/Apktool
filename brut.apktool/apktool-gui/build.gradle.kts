val apktoolVersion: String by rootProject.extra

plugins {
    alias(libs.plugins.shadow)
    application
}

dependencies {
    implementation(project(":brut.apktool:apktool-lib"))
    implementation(project(":brut.j.common"))
    implementation(project(":brut.j.dir"))
    implementation(project(":brut.j.util"))
}

application {
    mainClass.set("brut.apktool.gui.ApktoolGuiMain")
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=false")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "brut.apktool.gui.ApktoolGuiMain"
        attributes["Implementation-Version"] = apktoolVersion
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("apktool-gui")
    archiveVersion.set(apktoolVersion)
}
