import java.io.FileInputStream
import java.util.Properties

plugins {
    java
    signing
    `maven-publish`
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.spotless)
    alias(libs.plugins.git.version) apply false
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.omegat)
}

val dotgit = project.file(".git")
if (dotgit.exists()) {
    apply(plugin = libs.plugins.git.version.get().pluginId)
    val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
    val details = versionDetails()
    val baseVersion = details.lastTag.substring(1)
    version = when {
        details.isCleanTag -> baseVersion
        else -> baseVersion + "-" + details.commitDistance + "-" + details.gitHash + "-SNAPSHOT"
    }
} else {
    val gitArchival = project.file(".git-archival.properties")
    val props = Properties()
    props.load(FileInputStream(gitArchival))
    val versionDescribe = props.getProperty("describe")
    val regex = "^v\\d+\\.\\d+\\.\\d+$".toRegex()
    version = when {
        regex.matches(versionDescribe) -> versionDescribe.substring(1)
        else -> versionDescribe.substring(1) + "-SNAPSHOT"
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.BIN
    gradleVersion = "8.10"
}

omegat {
    version("6.0.0")
    pluginClass("org.omegat.connectors.machinetranslators.deepl.DeepLTranslate")
    packIntoJarFileFilter = {it.exclude("META-INF/**/*", "module-info.class", "kotlin/**/*")}
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.format.jdk14)
    compileOnly(libs.commons.io)
    compileOnly(libs.commons.text)

    // JSON parser
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.json.unit)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.slf4j.simple)

    testRuntimeOnly(libs.jackson.core)
    testRuntimeOnly(libs.jackson.databind)
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "org.omegat.machinetranslators.deepl")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "org.omegat"
            artifactId = "omegat-deepl-connector"
            pom {
                name.set("omegat-deepl-connector")
                description.set("DeepL V1 connector plugin")
                url.set("https://github.com/omegat-org/deepl-connector-plugin")
                licenses {
                    license {
                        name.set("The GNU General Public License, Version 3")
                        url.set("https://www.gnu.org/licenses/licenses/gpl-3.html")
                        distribution.set("repo")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/omegat-org/deepl-connector-plugin.git")
                    developerConnection.set("scm:git:git://github.com/omegat-org/deepl-connector-plugin.git")
                    url.set("https://github.com/omegat-org/deepl-connector-plugin")
                }
            }
        }
    }
}

val signKey = listOf("signingKey", "signing.keyId", "signing.gnupg.keyName").find { project.hasProperty(it) }
tasks.withType<Sign> {
    onlyIf { signKey != null && !rootProject.version.toString().endsWith("-SNAPSHOT") }
}

signing {
    when (signKey) {
        "signingKey" -> {
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
        "signing.keyId" -> { /* do nothing */
        }
        "signing.gnupg.keyName" -> {
            useGpgCmd()
        }
    }
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Javadoc>() {
    setFailOnError(false)
    options {
        jFlags("-Duser.language=en")
    }
}

nexusPublishing.repositories {
    sonatype()
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.WARN
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

spotless {
    java {
        target(listOf("src/*/java/**/*.java"))
        palantirJavaFormat()
        importOrder()
        removeUnusedImports()
        formatAnnotations()
    }
}
