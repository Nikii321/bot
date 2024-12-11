FROM openjdk:17
EXPOSE 8080
WORKDIR /app
ADD build/libs/bot-0.0.1-SNAPSHOT.jar vk-bot
ENTRYPOINT ["java", "-jar", "vk-bot"]