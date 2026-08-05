import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.xiaojingyu"
version = "0.1.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.foundation)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.xiaojingyu.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "XiaojingyuLikesYou"
            packageVersion = "0.1.0"
            description = "小鲸鱼喜欢你 - 天才猫娘AI白音桌面版"
            vendor = "xiaojingyu"
            windows {
                iconFile.set(project.file("src/main/resources/xiaojingyu_icon.ico"))
                menuGroup = "小鲸鱼喜欢你"
            }
        }
    }
}