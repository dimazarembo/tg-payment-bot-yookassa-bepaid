package com.dzarembo.testbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class MyTelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.providerToken}")
    private String providerToken;

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {

        // 1) Обработка pre_checkout_query — должно быть очень быстро
        if (update.hasPreCheckoutQuery()) {
            handlePreCheckout(update.getPreCheckoutQuery());
            // НЕ продолжать длительную работу до ответа
            return;
        }

        // 2) Сообщение с успешной оплатой
        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            long chatId = update.getMessage().getChatId();
            sendText(chatId, "✅ Оплата получена! Спасибо.");
            // тут можно сохранить данные платежа (update.getMessage().getSuccessfulPayment())
            return;
        }

        // 3) Обычные сообщения
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim().toLowerCase();
            long chatId = update.getMessage().getChatId();

            switch (text) {
                case "/start" -> sendText(chatId, "Привет! Напиши 'товар' чтобы получить инвойс.");
                case "товар" -> sendInvoice(chatId);
                default -> sendText(chatId, "Я понимаю /start и 'товар'");
            }
        }
    }

    private void handlePreCheckout(PreCheckoutQuery preCheckoutQuery) {
        AnswerPreCheckoutQuery answer = AnswerPreCheckoutQuery.builder()
                .preCheckoutQueryId(preCheckoutQuery.getId())
                .ok(true) // подтверждаем — платеж можно обрабатывать
                .build();
        try {
            // Ответ отправляем максимально быстро
            execute(answer);
            // Можно после ответа запустить асинхронную обработку (логирование, создание заказа и т.д.)
        } catch (TelegramApiException e) {
            e.printStackTrace();
            // Если хотим отклонить платеж — отправляем ok(false) и указать reason
            try {
                AnswerPreCheckoutQuery decline = AnswerPreCheckoutQuery.builder()
                        .preCheckoutQueryId(preCheckoutQuery.getId())
                        .ok(false)
                        .errorMessage("Ошибка на стороне сервера, попробуйте позже")
                        .build();
                execute(decline);
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendInvoice(long chatId) {
        try {
            SendInvoice invoice = SendInvoice.builder()
                    .chatId(chatId)
                    .title("Тестовый товар")
                    .description("Оплата встроенная в Telegram через ЮKassa")
                    .payload("order_" + System.currentTimeMillis())
                    .providerToken(providerToken)
                    .currency("RUB")
                    .prices(List.of(LabeledPrice.builder().label("Товар").amount(10000).build())) // копейки
                    .startParameter("test-payment")
                    .build();

            execute(invoice);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "Ошибка при создании инвойса.");
        }
    }

    private void sendText(long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
