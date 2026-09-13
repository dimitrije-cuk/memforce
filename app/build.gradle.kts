plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.memforce"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.memforce"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    // The framework's org.json is a stub in unit tests, so the real implementation comes first.
    testImplementation(libs.json)
    testImplementation(libs.junit)
}

/**
 * The question-set files these tests validate are read from the source tree rather than from the
 * test classpath, so Gradle cannot infer them as inputs. Without declaring them, editing a bundled
 * set leaves the validating test up to date and a malformed file reaches the APK unchecked.
 */
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/assets/question-sets"))
        .withPropertyName("bundledQuestionSets")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootProject.layout.projectDirectory.dir("docs/templates"))
        .withPropertyName("questionSetTemplates")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
