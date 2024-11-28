package com.vk.bot.controller

import com.fasterxml.jackson.databind.JsonNode
import com.vk.bot.service.MessageService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/callback")
class VkCallbackController(
    private val messageService: MessageService
) {

    @PostMapping
    fun handleCallback(@RequestBody event: JsonNode): String {
        return messageService.sendMessage(event)
    }
}
