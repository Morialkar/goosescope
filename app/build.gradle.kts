import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// REQUIRES_REVIEW: clé de publication (SPEC F22). signing/ est ignoré par git; le mot de passe est lu
// depuis un fichier pour n'apparaître ni dans le dépôt ni dans une commande (docs/RELEASING.md).
val releaseKeystore = rootProject.file("signing/goosescope-release.jks")
val releasePassword = rootProject.file("signing/password")
val releaseSigningReady = releaseKeystore.isFile && releasePassword.isFile

android {
    namespace = "com.morialkar.goosescope"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.morialkar.goosescope"
        minSdk = 33
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                val password = releasePassword.readText().trim()
                storeFile = releaseKeystore
                storePassword = password
                keyAlias = "goosescope"
                // Magasin PKCS12: la clé partage le mot de passe du magasin.
                keyPassword = password
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (releaseSigningReady) signingConfigs.getByName("release") else null
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    testImplementation(libs.junit4)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.ext.junit)
    // Compose tire Espresso 3.5, qui plante sur API 37 (InputManager.getInstance retiré): l'émulateur en a besoin.
    androidTestImplementation(libs.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
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
        // Branché sur assemble ET install: en T10, un install sans assemble avait laissé passer une permission.
        tasks.matching { it.name == "assemble$name" || it.name == "install$name" }.configureEach { dependsOn(verify) }
    }
}

tasks.register("verifyNoInternetPermission") {
    dependsOn("verifyDebugNoInternetPermission", "verifyReleaseNoInternetPermission")
}

// StringsParityTest lit les fichiers de ressources: sans cette entrée, Gradle réutilise un ancien résultat.
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/res").withPathSensitivity(PathSensitivity.RELATIVE)
}

// Sans clé, AGP produirait un APK release non signé: on refuse plutôt que de risquer de le publier.
tasks.matching { it.name == "assembleRelease" }.configureEach {
    doFirst {
        if (!releaseSigningReady) {
            throw GradleException("Signature de publication absente: signing/goosescope-release.jks et signing/password. Voir docs/RELEASING.md.")
        }
    }
}
