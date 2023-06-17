package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.apache.commons.lang3.time.DateUtils;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Date;

@Service
public class ChatService {

    private static final String LIST_OF_COMMANDS =
            "\n You can control me by sending these commands:\n" +
            "\n /create - to create a new notification" +
            "\n /list - to view the list of all your notifications" +
            "\n /reset - to reset me and discard notification's draft we may have made.";
    private static final String[] DATE_FORMATS = { "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy" };
    private static final String[] TIME_FORMATS = { "HH:mm" };
    final private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    final private ChatRepository chatRepository;
    final private NotificationRepository notificationRepository;

    public ChatService(ChatRepository chatRepository, NotificationRepository notificationRepository) {
        logger.info("ChatService constructor has been invoked");
        this.chatRepository = chatRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Handles /start-updates
     *
     * @return list of commands a user can apply.
     **/
    public String handleStart(Update update) {
        printMethodInfoLog("handleStart(Update update)", update);

        Message message = update.message();
        //If there is some uncompleted chat with the current chat id
        updateChatEntry(update, ChatStates.START, "");
        //The first component of the returned Sting is a tiny study sample
        //to introduce multi-language interface
        //'Ушедомс' means 'to start' in Mokshanian. 'Шумбрат' means 'Hello'
        return (message.text().equals("/ушедомс")) ? "Шумбрат, " : "Hello, " +
                message.chat().firstName() + "!\n" +
                "I can help you create a new notification for you and show list of your notifications.\n" +
                LIST_OF_COMMANDS;

    }

    /**
     * finds chat entry in database and updates it there
     *
     * @param update    telegram update to get all the details
     * @param chatState new state of user's chat
     * @param message   draft of notification message to the user
     */
    private void updateChatEntry(Update update, ChatStates chatState, String message) {

        Chat chat = update.message().chat();
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
                            ));
                        }
                );
    }

    /**
     * @return value ChatEntry.Languages based on the analysis of update's message's text
     */
    @NotNull
    private static Languages getLanguage(Update update) {
        return (update.message().text().equals("/ушедомс")) ?
                Languages.MOK : Languages.ENG;
    }

    /**
     * Handles all the updates for which there are no specified handler in the ChatServise class.
     **/
    public String handle(Update update) {
        printMethodInfoLog("handle(Update update)", update);

        ChatEntry chatEntry = getChatEntry(update);
        switch (chatEntry.getState()) {
            case INITIAL_STATE: return handleInitialStateByDefault(update);
            case INPUT_EVENT:   return handleInputEventState(update, chatEntry);
            case INPUT_DATE:    return handleInputDateState(update, chatEntry);
            default:
                logger.warn("There is not processed state within handle(update)");
                return "This piece of my algorithm is under construction:(";
        }
    }

    private String handleInputDateState(Update update, ChatEntry chatEntry) {
        printMethodInfoLog("handleInputDateState(Update update, ChatEntry chatEntry)", update);

        Date date = null;
        // Create an instance of SimpleDateFormat used for parsing/formatting
        // the string representation of date according to the chosen pattern
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMATS[0]);
        try {
            date = formatter.parse(update.message().text());
        }
        catch (ParseException e) {
            logger.info(chatEntry.getUserFirstName() + "\", DateTimeParseException is thrown with the message: {}", e.getMessage());
            return "It looks like you made a mistake writing the date.\n Your input was: \"" +
                    update.message().text() +
                    "\". \n Could you try to write once again please. " +
                    "The date format should be like \"" + DATE_FORMATS[0] + "\":";
        }

        logger.debug("date input by user = {}", date);
        System.out.println("date = " + date);

        //Check that date is not in past
        if (date.getTime() < (new Date()).getTime()-86_400_000) {
            return chatEntry.getUserFirstName() + ", date you entered belongs to the past. \nI expect to notify you" +
                    "only  in future. \nPlease, write me a correct one:";
        }
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());

        chatEntry.setDate(sqlDate);
        chatEntry.setState(ChatStates.INPUT_TIME);
        chatRepository.save(chatEntry);

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.
        String dateAsString = formatter.format(date);
        return "Good, you will be notified about \"" + chatEntry.getMessage() + "\" on \"" +
                dateAsString + "\".\n" +
                "Next, enter, please, the time you are going to be notified.\n" +
                "It should be in the next format: \n\"" +
                DATE_FORMATS[0] + "\": ";

    }

    /** Invoked in case when new user attempts to interact with the bot
     * but sends a text not equals to  '/start'
     *
     * @return the instruction to type '/start' to init dialog with the bot.
     */
    private String handleInitialStateByDefault(Update update) {
        printMethodInfoLog("handleInitialStateByDefault(Update update, ChatEntry chatEntry)", update);

        return "Hello, " + update.message().chat().firstName() + ":)" +
                "\n To start dialog with me just say /start !";
    }


    /**
     * Processes update when the chat is in the state of waiting from the user
     * what kind of event he would be like notified about.
     *
     * @param update    telegram update with all the information from the user
     * @param chatEntry chat entry instance we've been working with
     * @return instruction to the user what to do next
     */
    private String handleInputEventState(Update update, ChatEntry chatEntry) {
        printMethodInfoLog("handleInputEventState(Update update, ChatEntry chatEntry)", update);

        String notifiedEvent = update.message().text();
        if (notifiedEvent.isEmpty() || notifiedEvent.isBlank()) {
            return "I expect to get what are going to do that I should notify you about.\n" +
                    "It should be non-empty string kinda \"to do homework\" e.g., please:)";
        }
        //We suggested to the user to enter "/yes" to notify them about to do homework.
        if(notifiedEvent.equals("/yes")) {
            notifiedEvent = "to do homework";
        }
        //Set information about notified event into chat entry
        chatEntry.setMessage(notifiedEvent);
        //Switch chat entry into the stat "Waiting for date input"
        chatEntry.setState(ChatStates.INPUT_DATE);
        //Save chat information in db
        chatRepository.save(chatEntry);
        return "Well, you will be notified about \"" + notifiedEvent + "\".\n" +
                "Now, next, enter, please, the date when I should notify you.\n" +
                "It should be presented in the next format: \n\"" +
                DATE_FORMATS[0] + "\": ";

    }

    /**
     * @param update telegram update to obtain chat id from it.
     * @return Chat entry from database if it does exist out there or newly created instance.
     */
    private ChatEntry getChatEntry(Update update) {
        Chat chat = update.message().chat();
        return chatRepository.findById((chat.id()))
                //If chat entry is not presented in db then create a new one
                .orElseGet(() -> {
                            return chatRepository.save(new ChatEntry(
                                    chat.id(),
                                    chat.firstName()
                            ));
                        }
                );
    }

    /**
     * Handles update with text "/list".
     *
     * @return message containing all the notifications for the update's user.
     **/
    public String handleList(Update update) {
        printMethodInfoLog("handleList(Update update)", update);

        Long chatId = update.message().chat().id();

        Collection<Notification> notifications = notificationRepository.findByChat_id(chatId);
        if (notifications.isEmpty()) {
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

    /**
     * Handles update with text "/create".
     *
     * @return text containing instruction what user should enter to create a new reminder.
     **/

    public String handleCreate(Update update) {
        printMethodInfoLog("handleCreate(Update update)", update);

        updateChatEntry(update,
                ChatStates.INPUT_EVENT,
                ""
        );
        return "Alright, let's make it.\n " +
                "What kind of action to do should I notify you about?\n" +
                "I can guess it could be \"to do homework\", couldn't it be?\n" +
                "if it is so, type /yes, otherwise, write your option, just in form \"to do something useful\":)";
    }

    /**
     * Handles update with text "/reset". Acts almost similar to the method "/start".
     * "/reset" is introduced to make bot psychologically comfort.
     *
     * @return text containing instruction what user should enter to create a new reminder.
     **/
    public String handleReset(Update update) {
        printMethodInfoLog("handleRest(Update update)", update);

        //If there is some uncompleted chat with the current chat id
        updateChatEntry(update, ChatStates.START, "");
        return "I'm reset and ready to work:)\n" +
                LIST_OF_COMMANDS;

    }

    private void printMethodInfoLog(String methodSignature, Update update) {
        logger.info("{} with update.message().text()=\"{}\"",
                methodSignature,
                update.message().text());
    }
}
