plugins {
    kotlin("jvm") version "1.4.0" apply false
}

allprojects {
    group = "nl.jordie24.samics"
    version = "0.1.0"
}

subprojects {
    repositories {
        mavenCentral()
    }
}
