pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  // 注意这里修改了
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KPM-Manager"
include(":app")