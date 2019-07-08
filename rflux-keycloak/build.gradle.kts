plugins {
    idea
    kotlin("jvm")
}

group = "com.voidaspect"
version = "0.1"

repositories {
    jcenter()
    mavenLocal()
}

extra["springVersion"] = "5.1.5.RELEASE"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.security:spring-security-oauth2-resource-server:${property("springVersion")}")
    implementation("org.springframework.security:spring-security-oauth2-jose:${property("springVersion")}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test>() {
    useJUnitPlatform()
}