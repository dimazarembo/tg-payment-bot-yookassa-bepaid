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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
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

        if (update.hasPreCheckoutQuery()) {
            handlePreCheckout(update.getPreCheckoutQuery());
            return;
        }

        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            long chatId = update.getMessage().getChatId();
            sendText(chatId, "✅ Оплата получена! Спасибо 🙏");
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim().toLowerCase();
            long chatId = update.getMessage().getChatId();

            switch (text) {
                case "/start" -> showWelcomeMenu(chatId);
                case "yookassa" -> sendInvoice(chatId, "YooKassa", yookassaProviderToken, "RUB");
                case "bepaid" -> sendInvoice(chatId, "bePaid", bepaidProviderToken, "BYN");
                default -> sendText(chatId, "Пожалуйста, выберите способ оплаты, нажав кнопку ниже 👇");
            }
        }
    }

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

    /** Показ приветственного меню с кнопками */
    private void showWelcomeMenu(long chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("YooKassa"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("bePaid"));

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("""
                        👋 Привет!
                        Добро пожаловать в бот оплаты.
                        
                        Выберите способ оплаты:
                        """)
                .replyMarkup(keyboard)
                .build();

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendInvoice(long chatId, String providerName, String providerToken, String currency) {
        try {
            SendInvoice invoice = SendInvoice.builder()
                    .chatId(chatId)
                    .title("Оплата через " + providerName)
                    .description("Встроенная оплата в Telegram через " + providerName)
                    .payload("order_" + System.currentTimeMillis())
                    .providerToken(providerToken)
                    .currency(currency)
                    .prices(List.of(LabeledPrice.builder()
                            .label("Тестовый товар")
                            .amount(10000)
                            .build()))
                    .startParameter(providerName.toLowerCase().replaceAll("[^A-Za-z0-9_]", "") + "_payment")
                    .build();

            execute(invoice);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Ошибка при создании счёта (" + providerName + ")");
        }
    }

    private void sendText(long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
