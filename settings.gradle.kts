plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
    id("com.gradle.develocity") version "4.0.1"
}
develocity {
    buildScan {
        publishing.onlyIf { "true".equals(System.getProperty("envIsCi")) }
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}
rootProject.name = "deepl-connector-plugin"
