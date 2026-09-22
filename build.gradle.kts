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
        // El bundle WebAR se genera desde webar/src; analizar la copia de
        // assets duplicaria los hallazgos, y vendor/ es codigo de terceros.
        property(
            "sonar.exclusions",
            "app/src/main/assets/webar/**,webar/node_modules/**,**/build/**",
        )
    }
}
