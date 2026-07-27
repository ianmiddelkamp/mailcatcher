package dev.middelkamp.mailcatcher.repository;

import dev.middelkamp.mailcatcher.model.CapturedMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapturedMessageRepository extends JpaRepository<CapturedMessage, Long> {
    List<CapturedMessage> findAllByOrderByCreatedAtDescIdDesc();
}
