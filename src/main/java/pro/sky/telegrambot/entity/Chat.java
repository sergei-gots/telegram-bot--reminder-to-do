package pro.sky.telegrambot.entity;

import javax.persistence.*;

@Entity
//@Table("chats")
public class Chat {
    @Id
    //Value should be retrieved from telegram update.message.chat.id() in a listener.
    private long id;



    public enum States {
        START
    }


    public enum Languages {
        ENG,
        MOK
    }
    private States state;

    private Languages lang;

    private String userFirstName;

    private String message;

    public Chat() {
    }

    public Chat(long id, States state, Languages lang, String userFirstName, String message) {
        this.id = id;
        this.state = state;
        this.lang = lang;
        this.userFirstName = userFirstName;
        this.message = message;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public States getState() {
        return state;
    }

    public void setState(States state) {
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
