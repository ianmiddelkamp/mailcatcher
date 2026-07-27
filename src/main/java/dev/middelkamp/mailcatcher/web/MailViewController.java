package dev.middelkamp.mailcatcher.web;

import dev.middelkamp.mailcatcher.capture.MessageDeletionService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class MailViewController {

    private final CapturedMessageRepository messageRepository;
    private final CapturedAttachmentRepository attachmentRepository;
    private final MessageDeletionService deletionService;

    public MailViewController(
            CapturedMessageRepository messageRepository,
            CapturedAttachmentRepository attachmentRepository,
            MessageDeletionService deletionService) {
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
        this.deletionService = deletionService;
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

    @DeleteMapping("/message/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        deletionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages")
    public ResponseEntity<Void> deleteAllMessages() {
        deletionService.deleteAll();
        return ResponseEntity.noContent().build();
    }

    // HTML forms can't submit DELETE directly; these mirror the endpoints above for the UI's
    // delete buttons and redirect back to the index afterwards.
    @PostMapping("/message/{id}/delete")
    public String deleteMessageForm(@PathVariable Long id) {
        deletionService.deleteById(id);
        return "redirect:/";
    }

    @PostMapping("/messages/delete-all")
    public String deleteAllMessagesForm() {
        deletionService.deleteAll();
        return "redirect:/";
    }
}
