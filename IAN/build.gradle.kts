import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("kotlin")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
    id("info.solidsoft.pitest")
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

tasks.test {
    jvmArgs("-Xmx2g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    useJUnitPlatform()
}

val konsistCollect by tasks.registering {
    group = "build"
    description = "Runs all Konsist unit tests of all subprojects."
}

allprojects.filter { it.path.contains("konsist") }.forEach { project ->
    project.tasks.whenTaskAdded {
        if (name == "test") {
            konsistCollect.dependsOn(path)
        }
    }
}

tasks.assemble.dependsOn(konsistCollect)

detekt {
    source.setFrom(file("src/main/kotlin"))
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
    compileOnly(project(":IAN:annotations"))

    api(project(":IAN:enums"))
    api(project(":IAN:listener"))
    api(project(":IAN:packets"))
    api(project(":IAN:util"))
    api(project(":IAN:world"))

    api(libs.kotlin.stdlib)

    ksp(project(":IAN:processor"))
    ksp(libs.ksp.koin)

    implementation(libs.bundles.ian)

    runtimeOnly(libs.kotlin.reflect)

    testImplementation(testFixtures(project(":IAN:listener")))
    testImplementation(testFixtures(project(":IAN:packets")))
    testImplementation(testFixtures(project(":IAN:util")))
    testImplementation(libs.bundles.ian.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}

val pitestMutators: Set<String> by rootProject.extra
val pitestTimeoutFactor: BigDecimal by rootProject.extra

pitest {
    pitestVersion = libs.versions.pitest.asProvider()
    junit5PluginVersion = libs.versions.pitest.junit5
    verbose = true
    targetClasses = listOf("com.walkertribe.ian.*")
    threads = 2
    timeoutFactor = pitestTimeoutFactor
    outputFormats = listOf("HTML", "CSV")
    timestampedReports = false
    setWithHistory(true)
    mutators.addAll(pitestMutators)
}
