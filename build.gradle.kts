plugins {
    kotlin("jvm") version "2.2.20"
}

group = "io.momonga"
version = "0.1.0"

repositories { mavenCentral() }

dependencies {
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(17) }
