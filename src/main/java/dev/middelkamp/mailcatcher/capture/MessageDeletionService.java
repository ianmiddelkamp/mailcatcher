package dev.middelkamp.mailcatcher.capture;

import dev.middelkamp.mailcatcher.model.CapturedMessage;
import dev.middelkamp.mailcatcher.repository.CapturedMessageRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageDeletionService {

    private final CapturedMessageRepository repository;
    private final Path attachmentsDir;

    public MessageDeletionService(
            CapturedMessageRepository repository, @Value("${mailcatcher.attachments-dir}") String attachmentsDir) {
        this.repository = repository;
        this.attachmentsDir = Path.of(attachmentsDir);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            return;
        }
        deleteAttachmentDir(id);
        repository.deleteById(id);
    }

    public void deleteAll() {
        for (CapturedMessage message : repository.findAll()) {
            deleteAttachmentDir(message.getId());
        }
        repository.deleteAll();
    }

    private void deleteAttachmentDir(Long messageId) {
        Path dir = attachmentsDir.resolve(String.valueOf(messageId));
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
