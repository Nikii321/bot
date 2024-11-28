package com.vk.bot.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(VkData::class)
@EnableRetry
class AppConfig {
    @Bean
    fun RestTemplate(): RestTemplate {
        return org.springframework.web.client.RestTemplate()
    }
}