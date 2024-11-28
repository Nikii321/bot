package com.vk.bot.service

import com.fasterxml.jackson.databind.JsonNode

interface MessageService {
    fun sendMessage(event: JsonNode): String
}
