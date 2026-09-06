import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
	maven {
		name = "Shedaniel Maven"
		url = uri("https://maven.shedaniel.me/")
	}

	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/releases/")
	}
}

fabricApi {
	configureDataGeneration {
		client = true
	}
}

dependencies {
	// Mojang / Fabric Base
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API & Kotlin
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	// Cloth Config API (versão para 1.21.1)
	modApi("me.shedaniel.cloth:cloth-config-fabric:15.0.127") {
		exclude(group = "net.fabricmc.fabric-api")
	}

	// Mod Menu Integration
	modCompileOnly("com.terraformersmc:modmenu:11.0.1")

	// Cobblemon JAR
	modImplementation(files("libs/Cobblemon-fabric-1.8.4+1.21.1.jar"))

	val graalVersion = providers.gradleProperty("graal_version").get()
	modRuntimeOnly("org.graalvm.js:js:$graalVersion")
	modRuntimeOnly("org.graalvm.sdk:graal-sdk:$graalVersion")
	modRuntimeOnly("org.graalvm.regex:regex:$graalVersion")
	modRuntimeOnly("org.graalvm.truffle:truffle-api:$graalVersion")
	minecraftServerLibraries("com.ibm.icu:icu4j:${providers.gradleProperty("icu4j_version").get()}")

	// catchrate-display-fabric JAR
	modCompileOnly(files("libs/catchrate-display-fabric-2.12.3.jar"))
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
	}
}

java {
	withSourcesJar()
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
	javaLauncher = javaToolchains.launcherFor {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
}