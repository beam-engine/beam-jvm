package com.cs.beam.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.slf4j.LoggerFactory
import java.util.*


/**
 * @author  KK
 * @version 1.0
 *
 * Json util written on top of Jackson
 */

object JsonUtils {

    private val logger = LoggerFactory.getLogger(JsonUtils::class.java)
    private val typeRef: TypeReference<MutableMap<String, Any?>> = object : TypeReference<MutableMap<String, Any?>>() {}

    private lateinit var objectMapper: ObjectMapper

    init {
        try {
            objectMapper = initMapper()
        } catch (e: Exception) {
            logger.error("Problem while loading json config - {}", e.message)
        }
    }

    private fun initMapper(): ObjectMapper {
        val builder = JsonMapper.builder()
        return initMapperConfig(builder.build())
    }

    private fun initMapperConfig(objectMapper: ObjectMapper): ObjectMapper {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false)
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
        objectMapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        objectMapper.registerModule(JavaTimeModule())
        return objectMapper
    }

    private fun getAsString(jsonNode: JsonNode): String {
        return if (jsonNode.isTextual) jsonNode.textValue() else jsonNode.toString()
    }

    /**
     * Unmarshalling json to provided generic type
     *
     * @param json input json
     * @param type generic Type
     * @return <tt>generic object</tt>
     */
    fun <T> fromJson(json: String?, type: Class<T>?): T {
        return try {
            objectMapper.readValue(json, type)
        } catch (e: Exception) {
            throw JsonException("Error while converting json to object - $e")
        }
    }

    /**
     * Convert to Json from any object
     *
     * @param data Any object
     * @return <tt>Json string</tt>
     */
    fun toJson(data: Any?): String {
        Objects.requireNonNull(data)
        return try {
            objectMapper.writeValueAsString(data)
        } catch (e: JsonProcessingException) {
            throw JsonException("Error while unmarshalling json to object - $e")
        }
    }

    /**
     * Filter the corresponding json key with provided key path
     *
     * @param jsonData input json
     * @param keyPath  key to filter or find the corresponding node in input json
     * @return JsonNode [JsonNode]
     */
    fun filterJsonNodeByKeys(jsonData: String?, vararg keyPath: String?): JsonNode? {
        var jsonNode = fromJson(
            jsonData,
            JsonNode::class.java
        )
        for (key in keyPath) {
            jsonNode = if (jsonNode.has(key)) {
                jsonNode.path(key)
            } else {
                return null
            }
        }
        return jsonNode
    }

    /**
     * Find the json key if exist and return the value as string
     *
     * @param json input json
     * @param key  element to return as string
     * @return element value at the specified key
     */
    fun getAsString(json: String?, key: String?): String {
        var result = ""
        if (!json.isNullOrBlank()) {
            val jsonNode = filterJsonNodeByKeys(json, key)
            if (jsonNode != null) {
                result = getAsString(jsonNode)
            }
        }
        return result
    }

    /**
     * Remove the given key from the json input if exist
     *
     * @param json input json
     * @param key  element to remove
     * @return modified json with removed key
     */
    fun remove(json: String?, key: String?): String {
        return try {
            val node = objectMapper.readTree(json)
            (node as ObjectNode).remove(key)
            node.toString()
        } catch (e: JsonProcessingException) {
            throw JsonException("Problem while removing json key - " + e.message)
        }
    }

    fun createJsonNode(): ObjectNode = objectMapper.createObjectNode()

    fun createArrayNode(): ArrayNode = objectMapper.createArrayNode()

    fun <T> readValue(content: String, valueTypeRef: TypeReference<T>): T {
        return objectMapper.readValue(content, valueTypeRef)
    }

    fun toMap(obj: Any): MutableMap<String, Any?> {
        return objectMapper.convertValue(obj, typeRef)
    }

}