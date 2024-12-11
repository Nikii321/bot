package com.vk.bot

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vk.bot.config.VkData
import com.vk.bot.service.MessageServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.net.URI

class MessageServiceImplTest {

    private val vkData = VkData(
        confirmation = "test_confirmation",
        endpoint = "https://api.vk.com/method",
        access = "test_access_token",
        version = "5.131"
    )
    private val restTemplate = mockk<RestTemplate>()
    private val messageService = spyk(MessageServiceImpl(vkData, restTemplate), recordPrivateCalls = true)

    private val objectMapper = ObjectMapper()

    @Test
    fun `should return confirmation token on confirmation event`() {
        val eventJson = """
            {
              "type": "confirmation"
            }
        """
        val event = objectMapper.readTree(eventJson)

        val result = messageService.sendMessage(event)

        assertEquals("test_confirmation", result)
        verify { messageService["confirmBot"]() }
    }

    @Test
    fun `should process message_new event successfully`() {
        val peerId = 12345
        val message = "Hello"
        val eventJson = getEventJson(peerId, message)
        val event = objectMapper.readTree(eventJson)

        every {
            restTemplate.getForEntity(
                any<URI>(),
                eq(String::class.java)
            )
        } returns ResponseEntity.ok("{\"response\":100}")

        val result = messageService.sendMessage(event)

        assertEquals("OK", result)
        verify {
            messageService["messageNew"](
                withArg { node: JsonNode ->
                    assertEquals(peerId, node.get("peer_id").asInt())
                    assertEquals(message, node.get("text").asText())
                }
            )
        }
    }

    @Test
    fun `should throw exception when sending message fails`() {
        val peerId = 12345
        val message = "Hello"
        val eventJson = getEventJson(peerId, message)
        val event = objectMapper.readTree(eventJson)

        every { restTemplate.getForEntity(any<URI>(), any<Class<*>>()) } returns ResponseEntity(
            "error",
            HttpStatus.BAD_REQUEST
        )

        // Expect RuntimeException to be thrown
        assertThrows<RuntimeException> {
            messageService.sendMessage(event)
        }

        verify {
            messageService["messageNew"](
                withArg { node: JsonNode ->
                    assertEquals(peerId, node.get("peer_id").asInt())
                    assertEquals(message, node.get("text").asText())
                }
            )
        }
    }

    @Test
    fun `should call sendMessage failed via random_id`() {
        val peerId = 12345
        val message = "Hello"
        val eventJson = getEventJson(peerId, message)
        val event = objectMapper.readTree(eventJson)

        every {
            restTemplate.getForEntity(
                any<URI>(),
                eq(String::class.java)
            )
        } returns ResponseEntity.ok("{\"response\":100}")

        val result = messageService.sendMessage(event)
        assertEquals("OK", result)
        assertThrows<RuntimeException> { messageService.sendMessage(event) }
    }

    @Test
    fun `should handle unsupported event type`() {
        val eventJson = """
            {
              "type": "unsupported_event"
            }
        """
        val event = objectMapper.readTree(eventJson)

        val result = messageService.sendMessage(event)

        assertEquals("Unsupported event", result)
    }

    private fun getEventJson(peerId: Int, message: String): String {
        return """
        {
          "type": "message_new",
          "object": {
            "peer_id": $peerId,
            "text": "$message",
            "conversation_message_id":0,
            "from_id":0
          }
        }
    """
    }
}
