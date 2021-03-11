package no.nav.eessi.pensjon.json

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun mapAnyToJson(data: Any): String {
    return jacksonObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(data)
}

inline fun <reified T : Any> typeRefs(): TypeReference<T> = object : TypeReference<T>() {}

inline fun <reified T : Any> mapJsonToAny(json: String, objekt: TypeReference<T>, failonunknown: Boolean = false): T {
    return try {
        jacksonObjectMapper().configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failonunknown)
                .readValue(json, objekt)
    } catch (jpe: JsonParseException) {
        throw RuntimeException("Feilet ved konvertering av jsonformat, ${jpe.message}")
    } catch (jme: JsonMappingException) {
        throw RuntimeException("Feilet ved mapping av jsonformat, ${jme.message}")
    } catch (ex: Exception) {
        throw RuntimeException("Feilet med en ukjent feil ved jsonformat, ${ex.message}")
    }
}


fun Any.toJson() =  mapAnyToJson(this)
