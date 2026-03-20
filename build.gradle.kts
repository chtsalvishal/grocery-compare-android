plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Redirect build outputs outside OneDrive to avoid file-lock conflicts with OneDrive sync
allprojects {
    val buildBase = System.getenv("GRADLE_BUILD_DIR") ?: "C:/tmp/GroceryCompare"
    layout.buildDirectory.set(file("$buildBase/${project.name}/build"))
}
