import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing is optional so that anyone can build the project. Point AUTOMATIONS_KEYSTORE_PROPERTIES at a
// properties file (storeFile, storePassword, keyAlias, keyPassword) kept OUTSIDE the repository, or put a
// git-ignored keystore.properties in the project root. Without either, the release APK is left unsigned.
val keystoreProperties = Properties().apply {
    val file = providers.environmentVariable("AUTOMATIONS_KEYSTORE_PROPERTIES").orNull?.let(::file)
        ?: rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use(::load)
}

// The About screen shows the repository's license texts. They are copied from the repository root at build time,
// so the documents and the app cannot drift apart.
abstract class CopyLicenseAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val licenseFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        licenseFiles.files.forEach { it.copyTo(File(out, it.name), overwrite = true) }
    }
}

val copyLicenseAssets = tasks.register<CopyLicenseAssets>("copyLicenseAssets") {
    licenseFiles.from(
        rootProject.file("LICENSE"),
        rootProject.file("THIRD_PARTY_LICENSES.md"),
        rootProject.file("licenses/APACHE-2.0.txt"),
    )
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyLicenseAssets, CopyLicenseAssets::outputDir)
    }
}

android {
    namespace = "io.github.romanfabio.automations"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.romanfabio.automations"
        minSdk = 33
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
