package dev.middelkamp.mailcatcher.web;

import dev.middelkamp.mailcatcher.model.CapturedAttachment;
import dev.middelkamp.mailcatcher.model.CapturedMessage;
import dev.middelkamp.mailcatcher.repository.CapturedAttachmentRepository;
import dev.middelkamp.mailcatcher.repository.CapturedMessageRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class MailViewController {

    private final CapturedMessageRepository messageRepository;
    private final CapturedAttachmentRepository attachmentRepository;

    public MailViewController(
            CapturedMessageRepository messageRepository, CapturedAttachmentRepository attachmentRepository) {
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("messages", messageRepository.findAllByOrderByCreatedAtDescIdDesc());
        return "index";
    }

    @GetMapping("/message/{id}")
    public String message(@PathVariable Long id, Model model) {
        CapturedMessage message = messageRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No captured message " + id));
        model.addAttribute("message", message);
        return "message";
    }

    @GetMapping("/attachment/{id}")
    public ResponseEntity<Resource> attachment(@PathVariable Long id) throws IOException {
        CapturedAttachment attachment = attachmentRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No attachment " + id));

        Path path = Path.of(attachment.getStoredPath());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file missing on disk");
        }

        MediaType mediaType = attachment.getContentType() != null
                ? MediaType.parseMediaType(attachment.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFilename() + "\"")
                .body(new FileSystemResource(path));
    }
}
