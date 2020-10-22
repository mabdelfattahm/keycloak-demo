package keycloak.vanilla

import io.ktor.network.tls.certificates.generateCertificate
import java.io.File

object CertificateGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val jksFile = File("resources/temporary.jks").apply {
            parentFile.mkdirs()
        }

        if (!jksFile.exists()) {
            generateCertificate(jksFile, "SHA256withRSA", "keycloak-demo", keySizeInBits = 2048)
        }
    }
}