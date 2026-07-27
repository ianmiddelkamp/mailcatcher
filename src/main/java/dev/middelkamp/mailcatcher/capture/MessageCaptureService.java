package dev.middelkamp.mailcatcher.capture;

import dev.middelkamp.mailcatcher.model.CapturedAttachment;
import dev.middelkamp.mailcatcher.model.CapturedMessage;
import dev.middelkamp.mailcatcher.repository.CapturedMessageRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageCaptureService {

    private final CapturedMessageRepository repository;
    private final Path attachmentsDir;

    public MessageCaptureService(
            CapturedMessageRepository repository,
            @Value("${mailcatcher.attachments-dir}") String attachmentsDir) {
        this.repository = repository;
        this.attachmentsDir = Path.of(attachmentsDir);
    }

    public CapturedMessage capture(String endpointLabel, ParsedFrontMessage parsed) throws IOException {
        CapturedMessage message = new CapturedMessage();
        message.setEndpointLabel(endpointLabel);
        message.setSenderName(parsed.senderName());
        message.setSenderHandle(parsed.senderHandle());
        message.setToAddresses(join(parsed.to()));
        message.setCcAddresses(join(parsed.cc()));
        message.setBccAddresses(join(parsed.bcc()));
        message.setSubject(parsed.subject());
        message.setBody(parsed.body());
        message.setTags(join(parsed.tags()));

        message = repository.save(message);

        Path messageDir = attachmentsDir.resolve(String.valueOf(message.getId()));
        for (ParsedAttachment attachment : parsed.attachments()) {
            Files.createDirectories(messageDir);
            String safeFilename = Path.of(attachment.filename()).getFileName().toString();
            Path target = messageDir.resolve(safeFilename);
            Files.write(target, attachment.data());

            CapturedAttachment entity = new CapturedAttachment();
            entity.setMessage(message);
            entity.setFilename(safeFilename);
            entity.setContentType(attachment.contentType());
            entity.setSize(attachment.data().length);
            entity.setStoredPath(target.toString());
            message.getAttachments().add(entity);
        }

        return repository.save(message);
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(", ", values);
    }
}
