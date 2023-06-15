package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.function.Consumer;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    final private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            Long chatId= update.message().chat().id();
            logger.info("Processing update: text={}, chat-id={}",
                    update.message().text(),
                    chatId
            );
            // Process your updates here
            telegramBot.execute(new SendMessage(chatId,
                    "Hello, " + update.message().authorSignature() + "!"));
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
