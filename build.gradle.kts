// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}
buildscript {
    extra.apply {
        set("compileSdkVersion", 34)
        set("PUBLISH_GROUP_ID", "com.github.milanmaji")
        set("PUBLISH_ARTIFACT_ID", "android-triangle-seekbar")
    }
}