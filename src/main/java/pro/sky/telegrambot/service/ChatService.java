package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pro.sky.telegrambot.entity.Chat.Languages;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;

@Service
public class ChatService {

    final private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    public SendMessage handleStart(Update update) {
        logger.info("handleStart(update) with update.message().text()=\"{}\"",
                update.message().text());
        //If there is some uncompleted chat with the current chat id
        //then reset its data.
        //If not then create a new entry
        //return message
        Chat chat = update.message().chat();
        Languages lang =
                (update.message().text().equals("/ушедомс")) ?
                    Languages.MOK : Languages.ENG;
        String hello = (lang == Languages.ENG) ? "Hello, " : "Шумбрат, ";
        return new SendMessage(chat.id(),hello + chat.firstName() + "!");
    }

    public SendMessage handle(Update update) {
        logger.info("handle(update) with update.message().text()=\"{}\"",
                update.message().text());
        Chat chat = update.message().chat();
        //Try to find chat entry
        return new SendMessage(chat.id(), "this is next handler");
    }
}
