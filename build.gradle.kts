// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false

    // Google Services plugin
    // alias(libs.plugins.google.gms.google.services) apply false // Eğer TOML'dan yönetilecekse
}