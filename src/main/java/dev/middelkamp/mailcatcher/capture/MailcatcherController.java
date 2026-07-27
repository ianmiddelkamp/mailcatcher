package dev.middelkamp.mailcatcher.capture;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stands in for the real Front API surface FrontApp/FrontRequestHandler talk to. Only the two
 * message-send endpoints are actually captured; everything else gets a harmless stub response so
 * calls made during dev (contacts CRUD, channel/teammate/inbox listing) don't throw.
 */
@RestController
public class MailcatcherController {

    private final FrontFieldParser parser;
    private final MessageCaptureService captureService;

    public MailcatcherController(FrontFieldParser parser, MessageCaptureService captureService) {
        this.parser = parser;
        this.captureService = captureService;
    }

    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<Void> channelMessage(
            @org.springframework.web.bind.annotation.PathVariable String channelId, HttpServletRequest request)
            throws IOException, ServletException {
        captureService.capture("channel:" + channelId, parser.parse(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/inboxes/{inboxId}/imported_messages")
    public ResponseEntity<Void> importedMessage(
            @org.springframework.web.bind.annotation.PathVariable String inboxId, HttpServletRequest request)
            throws IOException, ServletException {
        captureService.capture("inbox:" + inboxId, parser.parse(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @RequestMapping(
            value = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> stub(HttpServletRequest request) {
        if (request.getMethod().equals(RequestMethod.PATCH.name())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"_results\":[]}");
    }
}
