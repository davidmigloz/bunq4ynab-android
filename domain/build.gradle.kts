plugins {
    applyLibDefaultPlugins()
}

android {
    kotlinOptions {
        applyKotlinConfig()
    }
    applyLibDefaultAndroidConfig()
    applyLibDefaultBuildTypesConfig()
}

dependencies {
    applyLibDefaultDependencies()
    implementation(project(":lib_utils"))
}
