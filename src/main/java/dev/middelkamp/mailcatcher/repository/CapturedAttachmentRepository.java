package dev.middelkamp.mailcatcher.repository;

import dev.middelkamp.mailcatcher.model.CapturedAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapturedAttachmentRepository extends JpaRepository<CapturedAttachment, Long> {
}
