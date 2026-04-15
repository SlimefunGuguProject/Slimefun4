import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gorylenko.GitPropertiesPluginExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.language.jvm.tasks.ProcessResources
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    `maven-publish`
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
    alias(libs.plugins.git.properties)
}

group = "com.github.slimefun"
version = resolveVersion()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.jsr305)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    compileOnly(libs.log4j.core)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.dough.api)
    implementation(libs.unirest.java) {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation(libs.hikaricp)
    implementation(libs.postgresql)

    compileOnly(libs.worldedit.core) { exclude(group = "*", module = "*") }
    compileOnly(libs.worldedit.bukkit) { exclude(group = "*", module = "*") }
    compileOnly(libs.mcmmo) { exclude(group = "*", module = "*") }
    compileOnly(libs.placeholderapi) { exclude(group = "*", module = "*") }
    compileOnly(libs.clearlag.core) { exclude(group = "*", module = "*") }
    compileOnly(libs.itemsadder.api) { exclude(group = "*", module = "*") }
    compileOnly(libs.orebfuscator.api) { exclude(group = "*", module = "*") }
    compileOnly(libs.vault.api) { exclude(group = "*", module = "*") }
    compileOnly(libs.authlib) { exclude(group = "*", module = "*") }

    implementation(libs.commons.lang)
    implementation(libs.slimefun.comp.lib)
    implementation(libs.guizhanlib.updater)
    implementation(libs.guizhanlib.minecraft)
}

tasks.jar {
    enabled = false
}

sourceSets.main {
    java.exclude("**/package-info.java")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

spotless {
    java {
        palantirJavaFormat("2.90.0").style("PALANTIR")
        removeUnusedImports()
        formatAnnotations()
    }
}

val buildVersion = version.toString()
val gitBuildTime: String? = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

configure<GitPropertiesPluginExtension> {
    keys = listOf(
        "git.build.time",
        "git.build.version",
        "git.commit.id.abbrev",
        "git.commit.id.full",
        "git.branch",
    )
    customProperty("git.build.version", buildVersion)
    customProperty("git.build.time", gitBuildTime)
    gitPropertiesName = "git.properties"
    gitPropertiesResourceDir = layout.buildDirectory.dir("generated/git-properties").get().asFile
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(tasks.named("generateGitProperties"))
    val pluginVersion = buildVersion
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand(mapOf("version" to pluginVersion))
    }
}

tasks.named("sourcesJar") {
    dependsOn(tasks.named("generateGitProperties"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("Slimefun")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    relocate("io.github.bakedlibs.dough", "io.github.thebusybiscuit.slimefun4.libraries.dough")
    relocate("io.papermc.lib", "io.github.thebusybiscuit.slimefun4.libraries.paperlib")
    relocate("kong.unirest", "io.github.thebusybiscuit.slimefun4.libraries.unirest")
    relocate("org.apache.commons.lang", "io.github.thebusybiscuit.slimefun4.libraries.commons.lang")
    relocate("net.guizhanss.guizhanlib", "io.github.thebusybiscuit.slimefun4.libraries.guizhanlib")
    /**exclude {
        it.path == "META-INF" || it.path.startsWith("META-INF/")
    }*/
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("sourcesJar"))
            groupId = project.group.toString()
            artifactId = "Slimefun"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            url = if (project.version.toString().contains("SNAPSHOT")) {
                uri("https://maven.norain.city/snapshots")
            } else {
                uri("https://maven.norain.city/releases")
            }
            credentials {
                username = System.getenv("MAVEN_ACCOUNT")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

fun Project.resolveVersion(): String {
    findProperty("projectVersion")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
    return "DEV-SNAPSHOT"
}
