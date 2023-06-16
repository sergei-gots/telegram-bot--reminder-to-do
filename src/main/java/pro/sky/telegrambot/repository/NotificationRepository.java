package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.entity.Notification;

import java.util.Collection;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Collection<Notification> findByChat_id(long id);
}
