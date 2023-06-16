package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pro.sky.telegrambot.entity.ChatEntry;
import pro.sky.telegrambot.entity.ChatEntry.Languages;
import pro.sky.telegrambot.entity.ChatEntry.ChatStates;
import pro.sky.telegrambot.entity.Notification;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.repository.ChatRepository;
import pro.sky.telegrambot.repository.NotificationRepository;

import java.util.Collection;

@Service
public class ChatService {

    final private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    final private ChatRepository chatRepository;
    final private NotificationRepository notificationRepository;

    public ChatService(ChatRepository chatRepository, NotificationRepository notificationRepository) {
        logger.info("ChatService constructor has been invoked");
        this.chatRepository = chatRepository;
        this.notificationRepository = notificationRepository;
    }

    /** Handles updates with texts like /start (can have other values depending on language
     * used by a user)
     * @return SendMessage to be displayed to the user
     **/
    public String handleStart(Update update) {
        logger.info("handleStart(update) with update.message().text()=\"{}\"",
                update.message().text());
        Chat chat = update.message().chat();
        //If there is some uncompleted chat with the current chat id
        updateChatEntry(chat, ChatStates.START, "");
        //The first component of the returned Sting is a tiny study sample
        //to introduce multi-language interface
        //'Ушедомс' means 'to start' in Mokshanian. 'Шумбрат' means 'Hello'
        return  (update.message().text().equals("/ушедомс")) ?  "Шумбрат, " : "Hello, " +
                chat.firstName() + "!\n" +
                "I can help you create a new notification for you and show list of your notifications.\n" +
                "\n You can control me by sending these commands:\n" +
                "\n /create - to create a new notification" +
                "\n /list - to view the list of all your notifications";
    }

    /** finds chat entry in database and updates it there
     *
     * @param chat telegram chat to get all the details
     * @param chatState new state of chat
     * @param message draft of notification message to the user
     */
    private void updateChatEntry(Chat chat, ChatStates chatState, String message) {
        ChatEntry resultChatEntry = chatRepository.findById((chat.id()))
                //reset chat's data.
                .map(chatEntry -> {
                    chatEntry.setState(chatState);
                    chatEntry.setMessage(message);
                    chatEntry.setUserFirstName(chat.firstName());
                    return chatRepository.save(chatEntry);
            })
                //If not then create a new entry
                .orElseGet(() -> {
                    return chatRepository.save(new ChatEntry(
                        chat.id(),
                        chat.firstName()
                    ));}
        );
    }

    /**
     * @return value ChatEntry.Languages based on the analysis of update's message's text
     */
    @NotNull
    private static Languages getLanguage(Update update) {
        return  (update.message().text().equals("/ушедомс")) ?
                    Languages.MOK : Languages.ENG;
    }

    /** Handles all the updates for which there are no specified handler in the ChatServise class.
     * **/
    public String handle(Update update) {
        logger.info("handle(update) with update.message().text()=\"{}\"",
                update.message().text());
        Chat chat = update.message().chat();
        //Try to find chat entry
        return "this is next handler";
    }

    /** Handles update with text "/list".
     * @return message containing all the notifications for the update's user.
     * **/
    public String handleList(Update update) {
        Long chatId = update.message().chat().id();

        Collection<Notification> notifications = notificationRepository.findByChat_id(chatId);
        if(notifications.isEmpty()) {
            return "You haven't got any reminders yet. Use the /create command to create a new reminder first.";
        }

        StringBuilder resultBuilder = new StringBuilder("Your have set the next reminders:\n");
        notifications.forEach(notification -> {
                    resultBuilder.append(notification.getTargetTime());
                    resultBuilder.append('\t');
                    resultBuilder.append(notification.getMessage());
                    resultBuilder.append('\n');

                });
        return resultBuilder.toString();
    }

    /** Handles update with text "/create".
     * @return text containing instruction what user should enter to create a new reminder.
     * **/

    public String handleCreate(Update update) {
        return "Alright, let's make it.\n " +
                "What kind of action to do should I notify you about?\n" +
                "I can guess it could be \"to do homework\", couldn't it be?\n" +
                "if it is so, type /yes, otherwise, write your option, just in form \"to do something useful\":)";
    }
}
