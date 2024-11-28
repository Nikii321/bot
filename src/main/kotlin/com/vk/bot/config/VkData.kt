package com.vk.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "vk.api")
class VkData(
    var access: String,
    var version: String,
    var endpoint: String,
    var confirmation: String
)
