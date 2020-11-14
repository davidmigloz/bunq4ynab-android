package app.bunq2ynab.data.bunq.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BunqInstallationIdDto(
    val id: String?
)

@JsonClass(generateAdapter = true)
internal data class BunqInstallationTokenDto(
    val id: Int?,
    val created: String?,
    val updated: String?,
    val token: String?
)

@JsonClass(generateAdapter = true)
internal data class BunqInstallationServerPublicKeyDto(
    val server_public_key: String?
)
