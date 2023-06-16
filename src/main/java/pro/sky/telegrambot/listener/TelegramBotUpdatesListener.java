package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.ChatService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    final private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    final private ChatService chatService;

    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            Long chatId= update.message().chat().id();
            String text = update.message().text();
            logger.info("Processing update: text={}, chat-id={}", text, chatId);
            // Process your updates here
            SendResponse sendResponse = telegramBot.execute(
                (text.equals("/start") || text.equals("/ушедомс"))?
                    chatService.handleStart(update) :
                    chatService.handle(update)
            );
            if(!sendResponse.isOk()) {
                logger.error("Some error during telegramBot.execute; sendResponse.errorCode()={},",
                        sendResponse.errorCode()
                );
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
