package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.Notification;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

@Service
public class NotificationReminderService {

    final private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    final private TelegramBot telegramBot;
    final private NotificationRepository notificationRepository;

    public NotificationReminderService(TelegramBot telegramBot, NotificationRepository notificationRepository) {
        logger.info("NotificationReminderService constructor has been invoked");
        this.telegramBot = telegramBot;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(fixedDelay = 60_000L)
    public void run() {
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        logger.info("run has been invoked. now ={}", currentTime);
        Collection<Notification> readyNotifications
                =  notificationRepository.findByTargetTime(currentTime);
        logger.info("Amount of notifications to sent = {}", readyNotifications.size());

        readyNotifications.forEach((notification) -> {
                    SendResponse sendResponse = telegramBot.execute(
                            new SendMessage(
                                    notification.getChat().getId(),
                                    "Hello, " + notification.getChat().getUserFirstName() +
                                            "!\nHere is your reliable bot."+
                                            "\n Now it is " + notification.getTargetTime() +
                                            " \n and I came to remind you " +
                                            "\n\tthat it is the right time \n\t\t" +
                                            notification.getMessage().toUpperCase() +
                                            "\t:) "
                            )
                    );
                    if (!sendResponse.isOk()) {
                        logger.error("Some error during telegramBot.execute; sendResponse.errorCode()={}, chatId={}",
                                sendResponse.errorCode(),
                                notification.getChat().getId()
                        );
                    }
        });
    }
}
