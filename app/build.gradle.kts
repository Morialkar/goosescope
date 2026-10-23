import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.morialkar.goosescope"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.morialkar.goosescope"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
}

// SPEC §8, STANDARDS §7: l'app ne doit jamais pouvoir accéder au réseau.
// Vérifie le manifeste fusionné (dépendances comprises), pas seulement le nôtre.
abstract class VerifyNoInternetPermission : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun verify() {
        val text = mergedManifest.get().asFile.readText()
        val forbidden = listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE")
            .filter { text.contains(it) }
        if (forbidden.isNotEmpty()) {
            throw GradleException("Permissions interdites dans le manifeste fusionné: $forbidden")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val verify = tasks.register<VerifyNoInternetPermission>("verify${name}NoInternetPermission") {
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        tasks.matching { it.name == "assemble$name" }.configureEach { dependsOn(verify) }
    }
}

tasks.register("verifyNoInternetPermission") {
    dependsOn("verifyDebugNoInternetPermission", "verifyReleaseNoInternetPermission")
}
