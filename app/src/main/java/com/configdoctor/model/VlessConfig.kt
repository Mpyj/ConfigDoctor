package com.configdoctor.model

data class VlessConfig(
    val rawLink: String,
    val uuid: String,
    val address: String,
    val port: Int,
    val sni: String?,
    val host: String?,
    val path: String?,
    val networkType: String,   // tcp, ws, grpc
    val security: String,      // tls, reality, none
    val fingerprint: String?,
    val flow: String?,
    val publicKey: String?,
    val shortId: String?
)