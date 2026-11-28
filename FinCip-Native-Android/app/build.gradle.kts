plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fincip.nativeapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fincip.nativeapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
        // Support-the-developer payment details. NOT stored in the repo: supply them as environment variables
        // (GitHub Actions secrets SUPPORT_UPI_ID / SUPPORT_PAYEE_NAME) or as gradle properties. Empty = feature shows "not set up".
        fun cfg(name: String) = (System.getenv(name) ?: (project.findProperty(name) as String?) ?: "").trim().replace("\\", "").replace("\"", "")
        buildConfigField("String", "SUPPORT_UPI_ID", "\"" + cfg("SUPPORT_UPI_ID") + "\"")
        buildConfigField("String", "SUPPORT_PAYEE_NAME", "\"" + cfg("SUPPORT_PAYEE_NAME") + "\"")
    }

    buildFeatures { compose = true; buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all {
            it.testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL; showStackTraces = false }
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("net.sourceforge.jexcelapi:jxl:2.6.12")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
}
