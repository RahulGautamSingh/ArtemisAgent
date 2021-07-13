import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("java-test-fixtures")
    id("kotlin")
    alias(libs.plugins.kover)
    id("info.solidsoft.pitest")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ksp)
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
    jvmArgs("-Xmx2g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    useJUnitPlatform()
}

tasks.assemble.dependsOn(":IAN:world:konsist:test")

detekt {
    source.setFrom(file("src/main/kotlin"))
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
    api(project(":IAN:enums"))
    api(project(":IAN:listener"))
    api(project(":IAN:util"))
    api(project(":IAN:vesseldata"))
    api(libs.kotlin.stdlib)

    ksp(project(":IAN:processor"))

    implementation(libs.kotlin.reflect)

    testFixturesApi(project(":IAN:listener"))
    testFixturesApi(project(":IAN:util"))
    testFixturesImplementation(libs.bundles.ian.world.test.fixtures)

    testImplementation(testFixtures(project(":IAN:util")))
    testImplementation(testFixtures(project(":IAN:vesseldata")))
    testImplementation(libs.bundles.ian.world.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)
}

kover {
    currentProject {
        sources {
            excludedSourceSets.add("testFixtures")
        }
    }
}

val pitestMutators: Set<String> by rootProject.extra

pitest {
    pitestVersion = libs.versions.pitest.asProvider()
    junit5PluginVersion = libs.versions.pitest.junit5
    verbose = true
    targetClasses = listOf("com.walkertribe.ian.world.*")
    threads = 2
    outputFormats = listOf("HTML", "CSV")
    timestampedReports = false
    setWithHistory(true)
    mutators.addAll(pitestMutators)
}
