package aethibo.io.core.security

import com.auth0.jwt.algorithms.Algorithm

object Cipher {
    val algorithm: Algorithm = Algorithm.HMAC512("kZnzR7iw463VyysmS9qln")

    fun encrypt(data: String?): ByteArray =
        algorithm.sign(data?.toByteArray())
}
