package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.entity.ChatEntry;

public interface ChatRepository extends JpaRepository<ChatEntry, Long> {
}
