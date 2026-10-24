import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// :core est du Kotlin pur, multiplateforme (IMPACT-IOS §2.1): la JVM pour Android, iOS pour
// l'iPhone, macOS seulement pour exécuter les tests sur le Mac sans simulateur.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Embarque des fichiers texte dans du code Kotlin commun (IMPACT-IOS §2.2): Kotlin/Native n'a pas de
 * ressources de classpath. Le texte est découpé en morceaux, car une constante de la JVM est limitée
 * à 64 Ko.
 */
abstract class EmbedTextFiles : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:Input
    abstract val objectName: Property<String>

    @get:Input
    abstract val single: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val root = sourceDir.get().asFile
        val files = root.walkTopDown().filter { it.isFile }.sortedBy { it.path }.toList()
        fun literal(text: String): String = text.chunked(8000).joinToString(", ") { chunk ->
            "\"" + chunk.replace("\\", "\\\\").replace("\"", "\\\"").replace("\$", "\\\$")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
        }
        val body = if (single.get().isNotEmpty()) {
            "    val json: String = listOf(${literal(root.resolve(single.get()).readText())}).joinToString(\"\")\n"
        } else {
            val entries = files.joinToString(",\n") { f ->
                "        \"${f.relativeTo(root).invariantSeparatorsPath}\" to listOf(${literal(f.readText())}).joinToString(\"\")"
            }
            "    private val files: Map<String, String> = mapOf(\n$entries,\n    )\n\n" +
                "    fun text(path: String): String = checkNotNull(files[path]) { \"Ressource de test absente: \$path\" }\n"
        }
        val out = outputDir.get().asFile.resolve("com/morialkar/goosescope/core/${objectName.get()}.kt")
        out.parentFile.mkdirs()
        out.writeText(
            "// Généré par :core:${name} — ne pas modifier.\npackage com.morialkar.goosescope.core\n\n" +
                "internal object ${objectName.get()} {\n$body}\n",
        )
    }
}

val generateCatalogSource = tasks.register<EmbedTextFiles>("generateCatalogSource") {
    sourceDir.set(layout.projectDirectory.dir("src/commonMain/resources"))
    objectName.set("BundledCatalogSource")
    single.set("catalog.json")
    outputDir.set(layout.buildDirectory.dir("generated/embedded/commonMain/kotlin"))
}

val generateTestResources = tasks.register<EmbedTextFiles>("generateTestResources") {
    sourceDir.set(layout.projectDirectory.dir("src/commonTest/resources"))
    objectName.set("TestResources")
    single.set("")
    outputDir.set(layout.buildDirectory.dir("generated/embedded/commonTest/kotlin"))
}

kotlin {
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        testRuns.named("test") { executionTask.configure { useJUnitPlatform() } }
    }

    val xcf = XCFramework("GoosescopeCore")
    iosArm64 {
        binaries.framework {
            baseName = "GoosescopeCore"
            isStatic = true
            xcf.add(this)
        }
    }
    macosArm64()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateCatalogSource.flatMap { it.outputDir })
            dependencies { implementation(libs.kotlinx.serialization.json) }
        }
        commonTest {
            kotlin.srcDir(generateTestResources.flatMap { it.outputDir })
            dependencies { implementation(kotlin("test")) }
        }
    }
}
