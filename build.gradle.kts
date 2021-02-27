plugins {
    `java-library`
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenLocal()
    maven {
        val releasesRepoUrl = uri("https://nexus.soma.salesforce.com/nexus/content/repositories/releases/")
        val snapshotsRepoUrl = uri("https://nexus.soma.salesforce.com/nexus/content/repositories/snapshots/")
        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
    }
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.18")
    implementation("io.vavr:vavr:0.10.3")
    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
}

group = "com.salesforce.ccspayments"
version = "2.2-SNAPSHOT"
description = "Vader"
java.sourceCompatibility = JavaVersion.VERSION_11

publishing {
    publications.create<MavenPublication>("mavenJava") {
        artifactId = "vader"
        from(components["java"])
        pom {
            name.set("Vader")
            description.set("An FP framework for Bean validation")
            url.set("https://git.soma.salesforce.com/CCSPayments/Vader")
            properties.set(mapOf(
                "maven.javadoc.failOnError" to "false"
            ))
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("gopala.akshintala@salesforce.com")
                    name.set("Gopal S Akshintala")
                    email.set("gopala.akshintala@salesforce.com")
                }
            }
            scm {
                connection.set("scm:git:https://git.soma.salesforce.com/ccspayments/vader")
                developerConnection.set("scm:git:git@git.soma.salesforce.com:ccspayments/vader.git")
                url.set("https://git.soma.salesforce.com/ccspayments/vader")
            }
        }
    }

    signing {
        sign(publishing.publications["mavenJava"])
    }

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }
}
