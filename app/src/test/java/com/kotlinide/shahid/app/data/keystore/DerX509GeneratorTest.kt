package com.kotlinide.shahid.app.data.keystore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date

class DerX509GeneratorTest {

    @Test
    fun testGenerateSelfSignedCert() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val now = System.currentTimeMillis()
        val notBefore = Date(now - 100000L)
        val notAfter = Date(now + 100000000L)
        val serial = BigInteger.valueOf(123456789L)
        val dName = "CN=Kotlin IDE Test, O=KotlinIDE, C=US"

        val derBytes = DerX509Generator.generateSelfSignedCert(
            keyPair = keyPair,
            dName = dName,
            serialNumber = serial,
            notBefore = notBefore,
            notAfter = notAfter
        )

        assertNotNull(derBytes)
        assertTrue(derBytes.isNotEmpty())

        val factory = CertificateFactory.getInstance("X.509")
        val cert = factory.generateCertificate(ByteArrayInputStream(derBytes)) as X509Certificate

        assertEquals(serial, cert.serialNumber)
        cert.verify(keyPair.public)
        assertTrue(cert.subjectDN.name.contains("Kotlin IDE Test"))
    }
}
