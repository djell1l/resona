pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-androidx")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: System.getenv("GH_USERNAME")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-material-components-android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: System.getenv("GH_USERNAME")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/oneui-design")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: System.getenv("GH_USERNAME")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
            }
        }
    }
}

rootProject.name = "Resona"
include(":app")
