package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pro.sky.telegrambot.entity.ChatEntry;
import pro.sky.telegrambot.entity.ChatEntry.Languages;
import pro.sky.telegrambot.entity.ChatEntry.ChatStates;
import pro.sky.telegrambot.entity.Notification;
import pro.sky.telegrambot.repository.ChatRepository;
import pro.sky.telegrambot.repository.NotificationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.TimeZone;

import static java.time.LocalDateTime.now;

@Service
public class ChatService {

    final private Logger logger = LoggerFactory.getLogger(ChatService.class);

    private static final String LIST_OF_COMMANDS =
            "\n You can control me by sending these commands:\n" +
                    "\n /create - to create a new notification" +
                    "\n /list - to view the list of all your notifications" +
                    "\n /reset - to reset me and discard notification's draft we may have made." +
                    "\n /author - to view info about my author.";
    private static final String[] DATE_FORMATS = {"dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy"};
    private static final String[] TIME_FORMATS = {"HH:mm"};

    // Create an instance of SimpleDateFormat used for parsing/formatting
    // the string representation of date according to the chosen pattern
    private final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
            .appendPattern(DATE_FORMATS[0]).toFormatter();
    // Create an instance of SimpleDateFormat used for parsing/formatting
    // the string representation of time according to the chosen pattern
    private final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
            /*.appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .toFormatter(Locale.getDefault());
*/
            .appendPattern(TIME_FORMATS[0]).toFormatter();

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
        chatRepository.findById((chat.id()))
                //reset chat's data.
                .map(chatEntry -> {
                    chatEntry.setState(chatState);
                    chatEntry.setMessage(message);
                    chatEntry.setUserFirstName(chat.firstName());
                    return chatRepository.save(chatEntry);
                })
                //If not then create a new entry
                .orElseGet(() -> chatRepository.save(new ChatEntry(
                                chat.id(),
                                chat.firstName()
                        ))
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
            case INITIAL_STATE:
                return handleInitialStateByDefault(update);
            case INPUT_EVENT:
                return parseWhatToDo(update, chatEntry);
            case INPUT_DATE:
                return parseInputDate(update, chatEntry);
            case INPUT_TIME:
                return parseInputTime(update, chatEntry);
            case START:
            case COMPLETED:
                return handleStateCompleted(update);
            default:
                logger.warn("DEVELOPER! There is not processed state within handle(update)");
                return "I really haven't got a clue what to do next...:( Please, /reset me!:)";
        }
    }

    private String handleStateCompleted(Update update) {
        printMethodInfoLog("handleInputCompleted(Update update)", update);
        updateChatEntry(update, ChatStates.START, "");
        return "Oops)) Hi, again. I almost fell asleep:)) What are we going to do, " +
                update.message().chat().firstName() +
                "?\n Here are my options below." +
                LIST_OF_COMMANDS;

    }

    private String parseInputTime(Update update, ChatEntry chatEntry) {
        printMethodInfoLog("parseInputTime(Update update, ChatEntry chatEntry)", update);

        LocalTime time = null;
        String text = update.message().text();

        if(text == null) {
            return chatEntry.getUserFirstName() + ", it looks like you forget to add text with the time to your message.\n" +
                    "I'd ask you to input the time you wish to be notified, please. \n Format is \"" +
                    TIME_FORMATS[0] + "\":";
        }

        //Considering an opt when user missed to input leading '0'-symbol for hours
        if(text.length() == TIME_FORMATS[0].length()-1) {
            text = "0" + text;
        }

        try {
            time = LocalTime.parse(text, timeFormatter);
        }
        catch (NullPointerException e) {

        }
        catch (DateTimeParseException e) {
            logger.info("DateTimeParseException while parsing input time = \"{}\" is thrown with the message: {}",
                    text, e.getMessage());
            return chatEntry.getUserFirstName() + ", it looks like you made a mistake writing the time.\n Your input was: \"" +
                    text + "\". \n Could you try to write once again please. " +
                    "The time format should be like \"" + TIME_FORMATS[0] + "\":";
        }

        logger.debug("time input by user = {}", time);

        logger.trace("time.getMinute()*60_000 =  {}", time.getMinute() * 60_000);
        logger.trace("time.getHour()*3_600_000 = {}", time.getHour() * 3_600_000);
        logger.trace("chatEntry.getDate().getTime() = {}", chatEntry.getDate());
        logger.trace("TimeZone.getDefault().getRawOffset() = {}", TimeZone.getDefault().getRawOffset());

        logger.trace("System.currentTimeMillis() = {}", System.currentTimeMillis());
        //Check that time is for more than one minute in future
        LocalDateTime dateTime = LocalDateTime.of(chatEntry.getDate(), time);
        ;

        if (dateTime.isBefore(now())) {
            return chatEntry.getUserFirstName() + ", date-n-time you entered is already in past. \n" +
                    "\nPlease, tell me another time:";
        }
        if (dateTime.isBefore(now().plus(1, ChronoUnit.MINUTES))) {
            return chatEntry.getUserFirstName() + ", date-n-time you entered are too near to be in past. \n" +
                    "\nPlease, tell me another time:";
        }

        chatEntry.setTime(time);
        chatEntry.setState(ChatStates.COMPLETED);
        chatRepository.save(chatEntry);

        notificationRepository.save(new Notification(
                0,
                chatEntry,
                dateTime,
                chatEntry.getMessage()));

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.
        String timeAsString = timeFormatter.format(time);
        String dateAsString = dateFormatter.format(chatEntry.getDate());
        return "Well, you will be certainly notified about \"" + chatEntry.getMessage() + "\" on \"" +
                dateAsString + "\" at \"" + timeAsString + "\":)\n\nSee ya!";

    }

    private String parseInputDate(Update update, ChatEntry chatEntry) {
        printMethodInfoLog("parseInputDate(Update update, ChatEntry chatEntry)", update);

        LocalDate date;
        String text = update.message().text();

        if ("/today".equals(text) || "today".equals(text)) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(text, dateFormatter);
            } catch (DateTimeParseException e) {
                logger.info("DateTimeParseException is thrown with the message: {}", e.getMessage());
                return chatEntry.getUserFirstName() + "\", It looks like you made a mistake writing the date." +
                        "\n Your input was: \"" + text +
                        "\". \n Could you try to write once again please. " +
                        "The date format should be like \"" + DATE_FORMATS[0] + "\":";
            }
            //Check that date is not in past
            if (date.isBefore(LocalDate.now())) {
                return chatEntry.getUserFirstName() + ", date you entered belongs to the past. \nI expect to notify you" +
                        "only  in future. \nPlease, write me a correct one:";
            }
        }
        logger.debug("User input={}, parsed date = {}", text, date);

        //Update chat-entry in db:
        chatEntry.setDate(date);
        chatEntry.setState(ChatStates.INPUT_TIME);
        chatRepository.save(chatEntry);

        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.
        String dateAsString = dateFormatter.format(date);
        return "Good, you will be notified about \"" + chatEntry.getMessage() + "\" on \"" +
                dateAsString + "\".\n" +
                "Next, enter, please, the time you are going to be notified.\n" +
                "It should be in the format: \n\"" +
                TIME_FORMATS[0] + "\": ";

    }

    /**
     * Invoked in case when new user attempts to interact with the bot
     * but sends a text not equals to  '/start'
     *
     * @return the instruction to type '/start' to init dialog with the bot.
     */
    private String handleInitialStateByDefault(Update update) {
        printMethodInfoLog("handleInitialStateByDefault(Update update, ChatEntry chatEntry)", update);

        return "Hello, " + update.message().chat().firstName() + ":)" +
                "\n To start dialog with me just say /start or /ушедомс !";
    }


    /**
     * Processes update when the chat is in the state of waiting from the user
     * what kind of event he would be like notified about.
     *
     * @param update    telegram update with all the information from the user
     * @param chatEntry chat entry instance we've been working with
     * @return instruction to the user what to do next
     */
    private String parseWhatToDo(Update update, ChatEntry chatEntry) {
        printMethodInfoLog("ParseWhatToDo(Update update, ChatEntry chatEntry)", update);

        String notifiedEvent = update.message().text();
        if (notifiedEvent == null || notifiedEvent.isEmpty() || notifiedEvent.isBlank()) {
            return "I expect to get what are going to do that I should notify you about.\n" +
                    "It should be non-empty string kinda \"to do homework\" e.g., please:)";
        }
        //We suggested to the user to enter "/yes" to notify them about to do homework.
        if ("/yes".equals(notifiedEvent) || "yes".equals(notifiedEvent)) {
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
                DATE_FORMATS[0] + "\" or just type /today for today's date: ";

    }

    /**
     * @param update telegram update to obtain chat id from it.
     * @return Chat entry from database if it does exist out there or newly created instance.
     */
    private ChatEntry getChatEntry(Update update) {
        Chat chat = update.message().chat();
        return chatRepository.findById((chat.id()))
                //If chat entry is not presented in db then create a new one
                .orElseGet(() -> chatRepository.save(new ChatEntry(
                                chat.id(),
                                chat.firstName()
                        ))
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

        StringBuilder resultBuilder = new StringBuilder("We have set the next reminders for you:\n\n");
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> {
            LocalDateTime targetTime = notification.getTargetTime();
            resultBuilder.append(targetTime);
            resultBuilder.append(
                    (targetTime.isBefore(now)) ?
                            "\t you had pleasure " :
                            "\t you will have pleasure ");
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
