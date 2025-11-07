package com.dzarembo.testbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class TestbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestbotApplication.class, args);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(MyTelegramBot bot) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        return botsApi;
    }

}
