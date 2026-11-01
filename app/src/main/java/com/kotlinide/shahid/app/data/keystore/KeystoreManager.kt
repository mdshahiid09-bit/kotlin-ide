package com.kotlinide.shahid.app.data.keystore

import android.content.Context
import android.util.Base64
import com.kotlinide.shahid.app.util.CrashReportingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date

data class GeneratedKeystore(
    val base64Content: String,
    val alias: String,
    val password: String,
    val localBackupFile: File?
)

class KeystoreManager(private val context: Context) {

    suspend fun generateReleaseKeystore(
        projectName: String,
        alias: String = "upload",
        keystorePassword: String,
        keyPassword: String = keystorePassword,
        dName: String = "CN=Kotlin IDE, O=Developer, C=US"
    ): Result<GeneratedKeystore> = withContext(Dispatchers.IO) {
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048, SecureRandom())
            val keyPair = keyPairGenerator.generateKeyPair()

            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(null, keystorePassword.toCharArray())

            val backupDir = File(context.getExternalFilesDir(null), "Keystores").apply { mkdirs() }
            val cleanName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val backupFile = File(backupDir, "${cleanName}_upload_keystore.jks")

            val cert = createSelfSignedCert(keyPair, dName)
            val certChain = arrayOf<Certificate>(cert)

            keyStore.setKeyEntry(
                alias,
                keyPair.private,
                keyPassword.toCharArray(),
                certChain
            )

            val baos = ByteArrayOutputStream()
            keyStore.store(baos, keystorePassword.toCharArray())
            val keystoreBytes = baos.toByteArray()

            FileOutputStream(backupFile).use { fos ->
                fos.write(keystoreBytes)
            }

            val base64String = Base64.encodeToString(keystoreBytes, Base64.NO_WRAP)

            Result.success(
                GeneratedKeystore(
                    base64Content = base64String,
                    alias = alias,
                    password = keystorePassword,
                    localBackupFile = backupFile
                )
            )
        } catch (e: Exception) {
            CrashReportingHelper.logHandledException("KeystoreManager", "Failed to generate release keystore", e)
            Result.failure(e)
        }
    }

    private fun createSelfSignedCert(
        keyPair: KeyPair,
        dName: String
    ): X509Certificate {
        val now = System.currentTimeMillis()
        val notBefore = Date(now - 24 * 60 * 60 * 1000L)
        val notAfter = Date(now + 10000L * 24 * 60 * 60 * 1000L) // ~27 years
        val serialNumber = BigInteger.valueOf(now)

        val derBytes = DerX509Generator.generateSelfSignedCert(
            keyPair = keyPair,
            dName = dName,
            serialNumber = serialNumber,
            notBefore = notBefore,
            notAfter = notAfter
        )

        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(ByteArrayInputStream(derBytes)) as X509Certificate
    }
}
