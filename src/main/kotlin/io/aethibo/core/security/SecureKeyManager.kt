package io.aethibo.core.security

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

data object SecureKeyManager {
    // Generate ES256 key pair (ECDSA with P-256 curve)
    fun generateES256KeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec("secp256r1") // P-256 curve
        keyGen.initialize(ecSpec, SecureRandom())
        return keyGen.generateKeyPair()
    }

    // Generate EdDSA key pair (Ed25519 - best security & performance)
    fun generateEdDSAKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("Ed25519")
        return keyGen.generateKeyPair()
    }

    // For development/testing - generate RSA keys (not recommended for production)
    fun generateRSA256KeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, SecureRandom())
        return keyGen.generateKeyPair()
    }
}
