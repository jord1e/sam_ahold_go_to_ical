plugins {
    kotlin("jvm")
}

dependencies {
    api("org.mnode.ical4j:ical4j:4.0.0-alpha2")
    implementation("net.sourceforge.htmlunit:htmlunit:2.41.0")
//    implementation("javax.cache:cache-api:1.1.1")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.5")
    implementation("com.github.ben-manes.caffeine:jcache:2.8.5")
//    implementation("org.jsr107.ri:cache-annotations-ri-cdi:1.1.0")
    implementation(kotlin("stdlib"))
}
