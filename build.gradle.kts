buildscript {

    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath(libs.gradle)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath(libs.kotlin.gradle)
        classpath(libs.dagger.hilt.gradle)
        classpath(libs.oss.licenses.plugin)
    }
}

val tasksStorePassword by extra("3tyd990")
val tasksKeyAlias by extra("Android-hady-key0")
val tasksKeyPassword by extra("3tyd990")
val tasksStoreFile by extra("C:\\Users\\akhap\\Documents\\GnuPG\\android-hady.jks")

tasks.getByName<Wrapper>("wrapper") {
    gradleVersion = "8.1.1"
    distributionType = Wrapper.DistributionType.ALL
}
