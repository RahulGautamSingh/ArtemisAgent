import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("java-library")
  id("kotlin")
  alias(libs.plugins.detekt)
}

val javaVersion: JavaVersion by rootProject.extra

java {
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}

tasks.compileKotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
    javaParameters = true
  }
}

detekt {
  source.setFrom(file("src/main/kotlin"))
  config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
  api(project(":IAN:annotations"))

  api(libs.kotlin.stdlib)
  api(libs.koin.annotations)
  api(libs.ksp.api)
  api(libs.kotlinpoet)

  implementation(project(":IAN:listener"))
  implementation(project(":IAN:util"))
  implementation(libs.kotlinpoet.ksp)
}
