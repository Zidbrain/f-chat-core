package io.github.zidbrain.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSerializationApi::class)
@Serializer(OffsetDateTime::class)
object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {

    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.from(formatter.parse(decoder.decodeString()))
    }
}