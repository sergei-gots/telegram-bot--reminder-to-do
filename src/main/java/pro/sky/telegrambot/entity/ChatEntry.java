package pro.sky.telegrambot.entity;

import javax.persistence.*;

@Entity
@Table(name="chats")
public class ChatEntry {
    @Id
    //Value should be retrieved from telegram update.message.chat.id() in a listener.
    private Long id;


    public enum ChatStates {
        START,
        INPUT_DATE, INPUT_EVENT
    }


    public enum Languages {
        ENG,
        MOK
    }


    private ChatStates state;

    private Languages lang;

    @Column(name = "user_first_name")
    private String userFirstName;

    private String message;

    public ChatEntry() {
    }

    public ChatEntry(Long id, ChatStates state, Languages lang, String userFirstName, String message) {
        this.id = id;
        this.state = state;
        this.lang = lang;
        this.userFirstName = userFirstName;
        this.message = message;
    }

    public ChatEntry(Long id, String userFirstName) {
        this(id, ChatStates.START, Languages.ENG, userFirstName, "");
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ChatStates getState() {
        return state;
    }

    public void setState(ChatStates state) {
        this.state = state;
    }

    public Languages getLang() {
        return lang;
    }

    public void setLang(Languages lang) {
        this.lang = lang;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "id=" + id +
                ", state=" + state +
                ", lang=" + lang +
                ", userFirstName='" + userFirstName + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

}
