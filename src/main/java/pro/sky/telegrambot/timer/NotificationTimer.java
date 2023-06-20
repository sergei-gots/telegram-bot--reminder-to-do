package pro.sky.telegrambot.timer;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.entity.Notification;
import pro.sky.telegrambot.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationTimer {

    final private Logger logger = LoggerFactory.getLogger(NotificationTimer.class);

    final private TelegramBot telegramBot;
    final private NotificationRepository notificationRepository;

    public NotificationTimer(TelegramBot telegramBot, NotificationRepository notificationRepository) {
        logger.info("NotificationReminderService constructor has been invoked");
        this.telegramBot = telegramBot;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void run() {
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        logger.info("run has been invoked. now = {}", currentTime);
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
