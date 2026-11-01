package com.kotlinide.shahid.app.data.keystore

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.Signature
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure Kotlin self-contained DER X.509 v3 self-signed certificate generator.
 * Eliminates deprecated/missing BouncyCastle dependencies on Android.
 */
object DerX509Generator {

    fun generateSelfSignedCert(
        keyPair: KeyPair,
        dName: String,
        serialNumber: BigInteger,
        notBefore: Date,
        notAfter: Date
    ): ByteArray {
        val tbsBytes = encodeTbsCertificate(
            publicKeyBytes = keyPair.public.encoded,
            dName = dName,
            serialNumber = serialNumber,
            notBefore = notBefore,
            notAfter = notAfter
        )

        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(keyPair.private)
            update(tbsBytes)
        }.sign()

        val sigAlgBytes = sha256WithRsaAlgorithmIdentifier()
        val sigBitString = encodeBitString(signature)

        return encodeSequence(tbsBytes + sigAlgBytes + sigBitString)
    }

    private fun encodeTbsCertificate(
        publicKeyBytes: ByteArray,
        dName: String,
        serialNumber: BigInteger,
        notBefore: Date,
        notAfter: Date
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Version: [0] EXPLICIT INTEGER (2 for v3)
        val v3Bytes = encodeInteger(BigInteger.valueOf(2))
        out.write(encodeExplicit(0, v3Bytes))

        // 2. Serial Number: INTEGER
        out.write(encodeInteger(serialNumber))

        // 3. Signature AlgorithmIdentifier
        out.write(sha256WithRsaAlgorithmIdentifier())

        // 4. Issuer Name
        val nameBytes = encodeDName(dName)
        out.write(nameBytes)

        // 5. Validity: SEQUENCE { notBefore UTCTime, notAfter UTCTime }
        val validityBytes = encodeValidity(notBefore, notAfter)
        out.write(validityBytes)

        // 6. Subject Name (same as issuer for self-signed)
        out.write(nameBytes)

        // 7. SubjectPublicKeyInfo (already DER encoded by KeyPair)
        out.write(publicKeyBytes)

        return encodeSequence(out.toByteArray())
    }

    private fun sha256WithRsaAlgorithmIdentifier(): ByteArray {
        // OID 1.2.840.113549.1.1.11
        val oidBytes = byteArrayOf(
            0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B
        )
        val encodedOid = encodeOid(oidBytes)
        val nullBytes = byteArrayOf(0x05, 0x00)
        return encodeSequence(encodedOid + nullBytes)
    }

    private fun encodeDName(dName: String): ByteArray {
        val pairs = dName.split(",").map { it.trim() }.filter { it.contains("=") }
        val rdnSeqOut = ByteArrayOutputStream()

        for (pair in pairs) {
            val parts = pair.split("=", limit = 2)
            val key = parts[0].trim().uppercase()
            val value = parts[1].trim()

            val oidBytes = when (key) {
                "CN" -> byteArrayOf(0x55, 0x04, 0x03)
                "O" -> byteArrayOf(0x55, 0x04, 0x0A)
                "OU" -> byteArrayOf(0x55, 0x04, 0x0B)
                "C" -> byteArrayOf(0x55, 0x04, 0x06)
                "L" -> byteArrayOf(0x55, 0x04, 0x07)
                "ST" -> byteArrayOf(0x55, 0x04, 0x08)
                else -> byteArrayOf(0x55, 0x04, 0x03)
            }

            val rdnAtv = encodeOid(oidBytes) + encodeUtf8String(value)
            val rdnSeq = encodeSequence(rdnAtv)
            val rdnSet = encodeSet(rdnSeq)
            rdnSeqOut.write(rdnSet)
        }

        return encodeSequence(rdnSeqOut.toByteArray())
    }

    private fun encodeValidity(notBefore: Date, notAfter: Date): ByteArray {
        val df = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val b1 = df.format(notBefore).toByteArray(Charsets.US_ASCII)
        val b2 = df.format(notAfter).toByteArray(Charsets.US_ASCII)

        val utcB1 = encodeHeader(0x17, b1.size) + b1
        val utcB2 = encodeHeader(0x17, b2.size) + b2
        return encodeSequence(utcB1 + utcB2)
    }

    fun encodeSequence(content: ByteArray): ByteArray = encodeHeader(0x30, content.size) + content
    fun encodeSet(content: ByteArray): ByteArray = encodeHeader(0x31, content.size) + content

    fun encodeExplicit(tag: Int, content: ByteArray): ByteArray =
        encodeHeader((0xA0 or tag), content.size) + content

    fun encodeInteger(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return encodeHeader(0x02, bytes.size) + bytes
    }

    fun encodeOid(bytes: ByteArray): ByteArray = encodeHeader(0x06, bytes.size) + bytes

    fun encodeUtf8String(str: String): ByteArray {
        val bytes = str.toByteArray(Charsets.UTF_8)
        return encodeHeader(0x0C, bytes.size) + bytes
    }

    fun encodeBitString(bytes: ByteArray): ByteArray {
        val content = ByteArray(bytes.size + 1)
        content[0] = 0x00 // No unused bits
        System.arraycopy(bytes, 0, content, 1, bytes.size)
        return encodeHeader(0x03, content.size) + content
    }

    private fun encodeHeader(tag: Int, length: Int): ByteArray {
        return when {
            length < 128 -> byteArrayOf(tag.toByte(), length.toByte())
            length <= 255 -> byteArrayOf(tag.toByte(), 0x81.toByte(), length.toByte())
            length <= 65535 -> byteArrayOf(
                tag.toByte(),
                0x82.toByte(),
                (length shr 8).toByte(),
                (length and 0xFF).toByte()
            )
            else -> throw IllegalArgumentException("Length too large for simple DER: $length")
        }
    }
}
