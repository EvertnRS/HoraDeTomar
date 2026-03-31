import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "br.upe.horaDeTomar"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.upe.horaDeTomar"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProperties = Properties().apply {
                val localPropertiesFile = rootProject.file("local.properties")
                if (localPropertiesFile.exists()) {
                    load(localPropertiesFile.inputStream())
                }
            }
            //storeFile = file(localProperties.getProperty("STORE_FILE", ""))
            //storePassword = localProperties.getProperty("STORE_PASSWORD", "")
            //keyPassword = localProperties.getProperty("KEY_PASSWORD", "")
            //keyAlias = localProperties.getProperty("KEY_ALIAS", "")
        }
    }

    buildTypes {
       getByName("debug") {
           applicationIdSuffix = ".debug"
           isMinifyEnabled = false
           isShrinkResources = false
           buildConfigField("String", "FHIR_URL_BASE", "\"http://localhost:8080/fhir/\"")
       }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
            //TODO trocar pela url da rnds
            buildConfigField("String", "FHIR_URL_BASE", "\"http://localhost:8080/fhir/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    // ignora esses arquivos do projeto, pois o gradle estava dizendo que mais de uma biblioteca estava adicionando eles no build
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime.android)
    implementation("androidx.work:work-runtime-ktx:2.10.3")

    implementation("com.google.code.gson:gson:2.10.1")
    // Hilt core
    implementation("com.google.dagger:hilt-android:2.56.2")
    implementation(libs.androidx.runtime)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")                  // <- fornece KeyboardOptions
    implementation("androidx.compose.foundation:foundation")  // BasicTextField, clickable
    implementation("androidx.compose.material3:material3")    // Surface, TextFieldDefaults, ModalBottomSheet

    // Ícones do Material (resolve o Icons.*):
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.runtime.saveable)
    implementation(libs.androidx.foundation.layout)

    ksp("com.google.dagger:hilt-compiler:2.56.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Para Jetpack Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.28.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("io.coil-kt:coil-compose:2.4.0")

    //Recursos Fhir
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:8.4.0")

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}