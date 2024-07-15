import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "1.5.15"
  id("xyz.jpenilla.run-paper") version "2.2.3" // Adds runServer and runMojangMappedServer tasks for testing
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.1.1" // Generates plugin.yml based on the Gradle config
}

group = "pl.zyczu.minecraft.paper"
version = "1.20.4-SNAPSHOT"
description = "Paper plugin for LongChat Fabric mod"

java {
  // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
  toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
  paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
  // paperweight.foliaDevBundle("1.20.4-R0.1-SNAPSHOT")
  // paperweight.devBundle("com.example.paperfork", "1.20.4-R0.1-SNAPSHOT")
}

tasks {
  // Configure reobfJar to run when invoking the build task
  assemble {
    dependsOn(reobfJar)
  }

  compileJava {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

    // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
    // See https://openjdk.java.net/jeps/247 for more information.
    options.release = 17
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
  }
}

// Configure plugin.yml generation
// - name, version, and description are inherited from the Gradle project.
bukkitPluginYaml {
  main = "pl.zyczu.minecraft.paper.longchat.LongChatPlugin"
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
  authors.add("Author")
  apiVersion = "1.20"
}
