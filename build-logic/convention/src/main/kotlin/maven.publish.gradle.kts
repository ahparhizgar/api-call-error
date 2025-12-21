plugins {
    id("com.vanniktech.maven.publish")
}

val projectGroup = project.findProperty("GROUP") as String
val projectVersion = project.findProperty("VERSION_NAME") as String

group = projectGroup
version = projectVersion

mavenPublishing {
    pom {
        name = project.findProperty("POM_NAME") as String
        description = project.findProperty("POM_DESCRIPTION") as String
        inceptionYear = project.findProperty("POM_INCEPTION_YEAR") as String
        url = project.findProperty("POM_URL") as String

        licenses {
            license {
                name = project.findProperty("POM_LICENSE_NAME") as String
                url = project.findProperty("POM_LICENSE_URL") as String
                distribution = project.findProperty("POM_LICENSE_DIST") as String
            }
        }

        developers {
            developer {
                id = project.findProperty("POM_DEVELOPER_ID") as String
                name = project.findProperty("POM_DEVELOPER_NAME") as String
                url = project.findProperty("POM_DEVELOPER_URL") as String
            }
        }

        scm {
            url = project.findProperty("POM_SCM_URL") as String
            connection = project.findProperty("POM_SCM_CONNECTION") as String
            developerConnection = project.findProperty("POM_SCM_DEV_CONNECTION") as String
        }
    }

    // Enable signing and publishing to Maven Central (defaults suffice without explicit SonatypeHost)
    publishToMavenCentral()
    signAllPublications()
}
