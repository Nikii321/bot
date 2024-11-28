package com.vk.bot

import com.fasterxml.jackson.databind.ObjectMapper
import com.vk.bot.controller.VkCallbackController
import com.vk.bot.service.MessageService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@ExtendWith(SpringExtension::class)
@WebMvcTest(VkCallbackController::class)
class VkCallbackControllerTest {
    @TestConfiguration
    class ControllerTestConfig {
        @Bean
        fun service() = mockk<MessageService>()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var messageService: MessageService
    private val objectMapper = ObjectMapper()

    @Test
    fun `should return confirmation token on confirmation event`() {
        val event = mapOf("type" to "confirmation")
        every { messageService.sendMessage(any()) } returns "213fefef3"

        mockMvc.post("/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(event)
        }.andExpect {
            status { isOk() }
            content { string("213fefef3") }
        }
    }

    @Test
    fun `should handle message_new event and return ok`() {
        val event = mapOf(
            "type" to "message_new",
            "object" to mapOf(
                "peer_id" to 12345,
                "text" to "Hello"
            )
        )
        every { messageService.sendMessage(any()) } returns "ok"

        mockMvc.post("/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(event)
        }.andExpect {
            status { isOk() }
            content { string("ok") }
        }
    }

    @Test
    fun `should return unsupported event for unknown event type`() {
        val event = mapOf("type" to "unknown_event")
        every { messageService.sendMessage(any()) } returns "Unsupported event"

        mockMvc.post("/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(event)
        }.andExpect {
            status { isOk() }
            content { string("Unsupported event") }
        }
    }
}
