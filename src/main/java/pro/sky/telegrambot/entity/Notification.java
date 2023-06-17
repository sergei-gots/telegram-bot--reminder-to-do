package pro.sky.telegrambot.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name="notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JoinColumn(name="chat_id")
    @ManyToOne
    private ChatEntry chat;

    Timestamp targetTime;

    String message;

    public Notification(long id,
                        ChatEntry chat,
                        Timestamp targetTime,
                        String message
    ) {
        this.id = id;
        this.chat = chat;
        this.targetTime = targetTime;
        this.message = message;
    }

    public Notification() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ChatEntry getChat() {
        return chat;
    }

    public void setChatId(ChatEntry chat) {
        this.chat = chat;
    }

    public Timestamp getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(Timestamp targetTime) {
        this.targetTime = targetTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", chat=" + chat +
                ", targetTime=" + targetTime +
                ", message='" + message + '\'' +
                '}';
    }
}
