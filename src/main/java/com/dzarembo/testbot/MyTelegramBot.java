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

    @Value("${telegram.bot.yookassaProvider}")
    private String yookassaProviderToken;

    @Value("${telegram.bot.bepaidProvider}")
    private String bepaidProviderToken;

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {

        // ✅ 1. Telegram требует подтверждать pre_checkout_query
        if (update.hasPreCheckoutQuery()) {
            handlePreCheckout(update.getPreCheckoutQuery());
            return;
        }

        // ✅ 2. успешная оплата
        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            long chatId = update.getMessage().getChatId();
            sendText(chatId, "✅ Оплата получена! Спасибо 🙏");
            return;
        }

        // ✅ 3. команды пользователя
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim().toLowerCase();
            long chatId = update.getMessage().getChatId();

            switch (text) {
                case "/start" -> sendText(chatId,
                        """
                        Привет! 👋
                        Доступные команды:
                        💳 yookassa — встроенная оплата через ЮKassa
                        💳 бипэйд — встроенная оплата через bePaid (Беларусь)
                        """);

                case "yookassa" -> sendInvoice(chatId, "ЮKassa", yookassaProviderToken, "RUB");

                case "бипэйд" -> sendInvoice(chatId, "bePaid", bepaidProviderToken, "BYN");

                default -> sendText(chatId, "Я понимаю команды /start, yookassa и бипэйд 🙂");
            }
        }
    }

    /** Подтверждаем pre_checkout_query — обязательно */
    private void handlePreCheckout(PreCheckoutQuery query) {
        try {
            execute(AnswerPreCheckoutQuery.builder()
                    .preCheckoutQueryId(query.getId())
                    .ok(true)
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /** Отправка счета через Telegram Payments API */
    private void sendInvoice(long chatId, String providerName, String providerToken, String currency) {
        try {
            String safeParam = providerName.equalsIgnoreCase("bePaid")
                    ? "bepaid_payment"
                    : "yookassa_payment";

            SendInvoice invoice = SendInvoice.builder()
                    .chatId(chatId)
                    .title("Тестовый товар (" + providerName + ")")
                    .description("Оплата встроенная в Telegram через " + providerName)
                    .payload("order_" + System.currentTimeMillis())
                    .providerToken(providerToken)
                    .currency(currency)
                    .prices(List.of(LabeledPrice.builder()
                            .label("Товар")
                            .amount(10000) // 100 BYN или RUB
                            .build()))
                    .startParameter(safeParam)
                    .build();

            execute(invoice);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Ошибка при создании счёта (" + providerName + ")");
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
