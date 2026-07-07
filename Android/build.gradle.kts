// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20-RC2" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.3.20-RC2" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20-RC2" apply false
}
buildscript {
  repositories {
      google()
  }
  dependencies {
      classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.9.7")
  }
}