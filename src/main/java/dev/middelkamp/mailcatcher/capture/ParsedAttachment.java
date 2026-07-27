package dev.middelkamp.mailcatcher.capture;

public record ParsedAttachment(String filename, String contentType, byte[] data) {
}
