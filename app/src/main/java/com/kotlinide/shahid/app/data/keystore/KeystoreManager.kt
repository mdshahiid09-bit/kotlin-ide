package com.kotlinide.shahid.app.data.keystore

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

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

            val cert = createSelfSignedCert(keyPair.public, keyPair.private, dName)
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
            Result.failure(e)
        }
    }

    private fun createSelfSignedCert(
        publicKey: java.security.PublicKey,
        privateKey: java.security.PrivateKey,
        dName: String
    ): X509Certificate {
        try {
            val builderClass = Class.forName("org.bouncycastle.x509.X509V3CertificateGenerator")
            val generator = builderClass.getDeclaredConstructor().newInstance()

            val setSerialNumberMethod = builderClass.getMethod("setSerialNumber", BigInteger::class.java)
            setSerialNumberMethod.invoke(generator, BigInteger.valueOf(System.currentTimeMillis()))

            val setIssuerDNMethod = builderClass.getMethod("setIssuerDN", X500Principal::class.java)
            setIssuerDNMethod.invoke(generator, X500Principal(dName))

            val setNotBeforeMethod = builderClass.getMethod("setNotBefore", Date::class.java)
            setNotBeforeMethod.invoke(generator, Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))

            val setNotAfterMethod = builderClass.getMethod("setNotAfter", Date::class.java)
            setNotAfterMethod.invoke(generator, Date(System.currentTimeMillis() + 10000L * 24 * 60 * 60 * 1000L))

            val setSubjectDNMethod = builderClass.getMethod("setSubjectDN", X500Principal::class.java)
            setSubjectDNMethod.invoke(generator, X500Principal(dName))

            val setPublicKeyMethod = builderClass.getMethod("setPublicKey", java.security.PublicKey::class.java)
            setPublicKeyMethod.invoke(generator, publicKey)

            val setSignatureAlgorithmMethod = builderClass.getMethod("setSignatureAlgorithm", String::class.java)
            setSignatureAlgorithmMethod.invoke(generator, "SHA256WithRSAEncryption")

            val generateMethod = builderClass.getMethod("generate", java.security.PrivateKey::class.java)
            return generateMethod.invoke(generator, privateKey) as X509Certificate
        } catch (e: Exception) {
            throw IllegalStateException("Failed to construct X509 certificate: ${e.message}", e)
        }
    }
}
