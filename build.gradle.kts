plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.sonarqube)
}

// projectKey y organization los genera SonarCloud al importar el repositorio
// desde GitHub. Si alli aparecen con otro valor, corregir aqui.
sonar {
    properties {
        property("sonar.projectKey", "Autonoma-semillero_Lectura-Aumentada-mobile")
        property("sonar.organization", "autonoma-semillero")
        property("sonar.projectName", "Lectura Aumentada Mobile")
        property("sonar.sourceEncoding", "UTF-8")
        // - assets/webar se genera desde webar/src: analizar la copia
        //   duplicaria cada hallazgo.
        // - El resto de assets (modelos .glb, audio, texturas) son binarios;
        //   Sonar intentaba leerlos como texto y avisaba de codificacion.
        // - Las extensiones binarias se excluyen ademas globalmente para que
        //   anadir recursos nuevos en otra carpeta no reintroduzca el ruido.
        property(
            "sonar.exclusions",
            listOf(
                "app/src/main/assets/webar/**",
                "app/src/main/assets/models/**",
                "app/src/main/assets/audio/**",
                "webar/node_modules/**",
                "**/build/**",
                "**/*.glb",
                "**/*.gltf",
                "**/*.mp3",
                "**/*.wav",
                "**/*.png",
                "**/*.jpg",
                "**/*.webp",
                "**/*.ttf",
            ).joinToString(","),
        )
    }
}
