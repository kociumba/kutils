import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
//    id("fabric-loom") version "1.7.1"
    id("fabric-loom") version "1.10.5"
    id("maven-publish")
    kotlin("plugin.serialization") version "1.8.10"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    accessWidenerPath = file("src/main/resources/kutils.accesswidener")

    mods {
        register("kutils") {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.

    maven(url = "https://repo.essential.gg/repository/maven-public")
    maven(url = "https://maven.wispforest.io")
//    maven(url = "https://repo.alignedcookie88.com/repository/maven-public/")
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.hypixel.net/repository/Hypixel/")
    maven(url = "https://maven.terraformersmc.com/")

    maven {
        url = uri("https://maven.pkg.github.com/kociumba/imgui-mc")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
        }
    }
}

tasks.register("printEnvironment") {
    doLast {
        System.getenv().forEach { t, u ->
            println("$t -> $u")
        }
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    implementation(include("gg.essential:vigilance:${project.property("vigilance_version")}")!!)
    implementation(include("gg.essential:elementa:${project.property("elementa_version")}")!!)
    modImplementation(include("gg.essential:universalcraft-1.21-fabric:383+feature-enable-jvmdefault-all")!!)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // would be cool to be able to include it in jar couse of fucked up versioning
    // if not we have to declare dependency on version down like this: "imguimc": "1.21.1-1.0.7"
    //  which sucks couse the new one has debug tools
//    modImplementation(include("xyz.breadloaf.imguimc:imgui-mc:${project.property("imguimc_version")}")!!)

    // using my fork couse the other one is down
    modImplementation(include("xyz.breadloaf.imguimc:imgui-mc:1.21.1-1.0.15")!!)
//    modImplementation(include("net.hypixel:mod-api:1.0.1")!!)

    implementation("com.github.weisj:jsvg:1.6.0")

    implementation("com.github.only52607.luakt:luakt-core:2.6.1")
    implementation("com.github.only52607.luakt:luakt-extension:2.6.1")
    implementation(include("org.luaj:luaj-jse:3.0.1")!!)

    modCompileOnly("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    testImplementation("net.fabricmc:fabric-loader-junit:${project.property("loader_version")}")

}


tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
            "kotlin_loader_version" to project.property("kotlin_loader_version")
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
