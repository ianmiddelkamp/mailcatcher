package dev.middelkamp.mailcatcher.capture;

import java.util.List;

public record ParsedFrontMessage(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String body,
        String senderName,
        String senderHandle,
        List<String> tags,
        List<ParsedAttachment> attachments) {
}
