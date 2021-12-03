
buildscript {
    val kotlinVersion = "1.5.31"

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        // keeping this here to allow AS to automatically update
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    }
}

repositories {
    google()
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
