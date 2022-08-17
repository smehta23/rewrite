plugins {
    id("org.openrewrite.java-library")
}

dependencies {
    implementation(project(":rewrite-java"))
    implementation(project(":rewrite-test"))
    implementation(project(":rewrite-visualizer"))


    implementation("org.assertj:assertj-core:latest.release")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks.withType<Javadoc> {
    isFailOnError = false
    exclude("org/openrewrite/java/**")
}
