package com.vk.bot.service

import com.fasterxml.jackson.databind.JsonNode
import com.vk.bot.config.VkData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class MessageServiceImpl(
    private val vkData: VkData,
    private val restTemplate: RestTemplate
) : MessageService {
    private val log: Logger = LoggerFactory.getLogger(MessageService::class.java)


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
            else -> "Unsupported event".also { log.info("Unsupported event: {}", eventType) }
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
        sendRequest(peerId, message)
        return "ok"
    }

    @Retryable(maxAttempts = 5, backoff = Backoff(delay = 1000))
    private fun sendRequest(peerId: Int, message: String) {
        val message = if (!message.isBlank()) "Вы сказали: $message" else "Вы ничего не сказали"
        val url = UriComponentsBuilder.fromUriString("${vkData.endpoint}/messages.send")
            .queryParam("access_token", vkData.access)
            .queryParam("v", vkData.version)
            .queryParam("peer_id", peerId)
            .queryParam("message", message)
            .build()
            .toUri()
        val response = restTemplate.getForEntity(url, String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("message send successful {}, {}", peerId)
        } else {
            log.error("Error during message_new ")
            throw RuntimeException(response.body)
        }
    }
}
