import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("java-library")
  id("kotlin")
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

tasks.test {
  useJUnitPlatform()
}

dependencies {
  testImplementation(libs.bundles.konsist.common)
  testRuntimeOnly(libs.bundles.konsist.runtime)
}
