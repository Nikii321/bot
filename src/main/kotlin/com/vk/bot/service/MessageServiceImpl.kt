package com.vk.bot.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.vk.bot.config.VkData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Service
class MessageServiceImpl(
    private val vkData: VkData,
    private val restTemplate: RestTemplate
) : MessageService {
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val log: Logger = LoggerFactory.getLogger(MessageService::class.java)
    private val cacheEvent = ConcurrentHashMap<Int, Int>()

    override fun sendMessage(event: JsonNode): String {
        event.get("type")
        val eventType = event.get("type").asText()
        val eventObject = event.get("object")
        return when (eventType) {
            "confirmation" -> confirmBot().also { log.info("confirmation") }
            "message_new" -> messageNew(eventObject).also {
                log.info(
                    "message_new, {}",
                    eventObject.get("conversation_message_id").asInt()
                )
            }
            "message_reply" -> "OK".also { log.info(event.toPrettyString()) }
            else -> "OK".also { log.info("Unsupported event: {}", eventType) }
        }
    }

    private fun confirmBot(): String {
        return vkData.confirmation
    }

    private fun getPeerId(eventObject: JsonNode): Int {
        val peer_id = eventObject.get("peer_id")
        if (peer_id.isNull && peer_id.isEmpty) {
            return eventObject.get("user_id").asInt()
        }
        return peer_id.asInt()
    }

    private fun messageNew(eventObject: JsonNode): String {
        val peerId = getPeerId(eventObject)
        val message = eventObject.get("text").asText()
        val fromId = eventObject.get("from_id").asInt()
        sendMessage(fromId, peerId, message)
        return "OK"
    }

    @Retryable(retryFor = [RuntimeException::class], maxAttempts = 5, backoff = Backoff(delay = 1000))
    private fun sendMessage(fromId: Int, peerId: Int, message: String) {
        val randomId = Random.nextInt(Integer.MAX_VALUE)
        val message = if (!message.isBlank()) "Вы сказали: $message" else "Вы ничего не сказали"
        val url = UriComponentsBuilder.fromUriString("${vkData.endpoint}/messages.send")
            .queryParam("access_token", vkData.access)
            .queryParam("v", vkData.version)
            .queryParam("peer_id", peerId)
            .queryParam("message", message)
            .queryParam("random_id", randomId)
            .build()
            .toUri()
        val response = restTemplate.getForEntity(url, String::class.java)
        validRequest(fromId, response)
        cacheEvent.put(fromId, response.getResponseCode())
        log.info("${response.body}")
    }

    private fun validRequest(fromId: Int, response: ResponseEntity<String>) {
        if (!response.statusCode.is2xxSuccessful) {
            log.error("Error during sending message, ${response.body}")
            throw RuntimeException(response.body)
        }
        if (cacheEvent[fromId] == response.getResponseCode()) {
            log.error("random id should be unique, ${response.body}")
            throw RuntimeException(response.body)
        }
    }

    private fun ResponseEntity<String>.getResponseCode(): Int {
        return objectMapper.readTree(this.body).get("response").asInt()
    }
}
