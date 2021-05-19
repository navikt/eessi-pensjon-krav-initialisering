package no.nav.eessi.pensjon.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.TextNode
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JsonDateSerializer : JsonSerializer<LocalDateTime?>() {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    @Throws(IOException::class, JsonProcessingException::class)

    override fun serialize(value: LocalDateTime?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        val dateString = value?.format(formatter)
        gen?.writeString(dateString)
    }
}

class JsonDateDeserializer : JsonDeserializer<LocalDateTime?>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): LocalDateTime {
        val oc: ObjectCodec = jp.codec
        val node = oc.readTree(jp) as TextNode
        val dateString = node.textValue()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.parse(dateString, formatter)
    }
}
