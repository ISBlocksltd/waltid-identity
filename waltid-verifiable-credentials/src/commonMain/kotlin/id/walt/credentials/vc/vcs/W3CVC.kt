package id.walt.credentials.vc.vcs

import id.walt.credentials.schemes.JwsSignatureScheme
import id.walt.credentials.schemes.JwsSignatureScheme.JwsHeader
import id.walt.credentials.schemes.JwsSignatureScheme.JwsOption
import id.walt.crypto.keys.Key
import id.walt.crypto.utils.JsonUtils.toJsonElement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class W3CVCSerializer : KSerializer<W3CVC> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor
    override fun deserialize(decoder: Decoder): W3CVC = W3CVC(decoder.decodeSerializableValue(JsonObject.serializer()))
    override fun serialize(encoder: Encoder, value: W3CVC) = encoder.encodeSerializableValue(JsonObject.serializer(), value.toJsonObject())
}

@Serializable(with = W3CVCSerializer::class)
data class W3CVC(
    private val content: Map<String, JsonElement> = emptyMap()
) : Map<String, JsonElement> by content {


    fun toJsonObject(): JsonObject = JsonObject(content)
    fun toJson(): String = Json.encodeToString(content)
    fun toPrettyJson(): String = prettyJson.encodeToString(content)


    suspend fun signJws(
        issuerKey: Key,
        issuerDid: String,
        subjectDid: String,
        /** Set additional options in the JWT header */
        additionalJwtHeader: Map<String, String> = emptyMap(),
        /** Set additional options in the JWT payload */
        additionalJwtOptions: Map<String, JsonElement> = emptyMap()
    ): String {
        return JwsSignatureScheme().sign(
            data = this.toJsonObject(),
            key = issuerKey,
            jwtHeaders = mapOf(
                JwsHeader.KEY_ID to issuerDid,
                *(additionalJwtHeader.entries.map { it.toPair() }.toTypedArray())
            ),
            jwtOptions = mapOf(
                JwsOption.ISSUER to JsonPrimitive(issuerDid),
                JwsOption.SUBJECT to JsonPrimitive(subjectDid),
                *(additionalJwtOptions.entries.map { it.toPair() }.toTypedArray())
            ),
        )
    }

    companion object {
        fun build(
            context: List<String>,
            type: List<String>,
            vararg data: Pair<String, Any>
        ): W3CVC {
            return W3CVC(
                mutableMapOf(
                    "@context" to context.toJsonElement(),
                    "type" to type.toJsonElement()
                ).apply { putAll(data.toMap().mapValues { it.value.toJsonElement() }) }
            )
        }


        fun fromJson(json: String) =
            W3CVC(Json.decodeFromString<Map<String, JsonElement>>(json))

        private val prettyJson = Json { prettyPrint = true }
    }

}
