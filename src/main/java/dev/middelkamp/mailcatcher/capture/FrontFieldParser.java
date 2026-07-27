package dev.middelkamp.mailcatcher.capture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parses the two request shapes FrontApp actually sends: a JSON body (application/json) or
 * FrontApp's manually-built multipart body (postMultiPart), which uses bracketed field names
 * like to[0], options[tags][0], sender[name], attachments[0].
 */
@Component
public class FrontFieldParser {

    private static final Pattern BRACKET_TOKEN = Pattern.compile("\\[([^\\]]*)]");

    private final ObjectMapper objectMapper;

    public FrontFieldParser() {
        this.objectMapper = new ObjectMapper();
    }

    public ParsedFrontMessage parse(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            return parseMultipart(request);
        }
        return parseJson(request);
    }

    private ParsedFrontMessage parseJson(HttpServletRequest request) throws IOException {
        JsonNode root = objectMapper.readTree(request.getInputStream());

        List<String> to = readStringArray(root, "to");
        List<String> cc = readStringArray(root, "cc");
        List<String> bcc = readStringArray(root, "bcc");
        String subject = readText(root, "subject");
        String body = readText(root, "body");

        String senderName = readText(root, "sender_name");
        String senderHandle = null;
        JsonNode sender = root.get("sender");
        if (sender != null && sender.isObject()) {
            if (senderName == null) {
                senderName = readText(sender, "name");
            }
            senderHandle = readText(sender, "handle");
        }

        List<String> tags = new ArrayList<>();
        JsonNode options = root.get("options");
        if (options != null) {
            tags.addAll(readStringArray(options, "tags"));
        }

        // Attachments are only sent via multipart in this codebase (postMultiPart), but handle a
        // JSON attachments array defensively in case a caller ever sends inline base64 payloads.
        List<ParsedAttachment> attachments = new ArrayList<>();
        JsonNode attachmentsNode = root.get("attachments");
        if (attachmentsNode != null && attachmentsNode.isArray()) {
            for (JsonNode attachment : attachmentsNode) {
                String filename = readText(attachment, "filename");
                String mimetype = readText(attachment, "mimetype");
                JsonNode dataNode = attachment.get("data");
                byte[] data = dataNode != null ? dataNode.asText("").getBytes() : new byte[0];
                attachments.add(new ParsedAttachment(filename, mimetype, data));
            }
        }

        return new ParsedFrontMessage(to, cc, bcc, subject, body, senderName, senderHandle, tags, attachments);
    }

    private ParsedFrontMessage parseMultipart(HttpServletRequest request) throws IOException, ServletException {
        List<String> to = new ArrayList<>();
        List<String> cc = new ArrayList<>();
        List<String> bcc = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        List<ParsedAttachment> attachments = new ArrayList<>();
        String[] subject = {null};
        String[] body = {null};
        String[] senderName = {null};
        String[] senderHandle = {null};

        for (Part part : request.getParts()) {
            String name = part.getName();
            if (name == null) {
                continue;
            }
            boolean isFile = part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty();
            if (isFile) {
                attachments.add(new ParsedAttachment(
                        part.getSubmittedFileName(),
                        part.getContentType(),
                        part.getInputStream().readAllBytes()));
                continue;
            }

            String value = new String(part.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            List<String> tokens = tokenize(name);
            String base = tokens.get(0);

            switch (base) {
                case "to" -> to.add(value);
                case "cc" -> cc.add(value);
                case "bcc" -> bcc.add(value);
                case "subject" -> subject[0] = value;
                case "body" -> body[0] = value;
                case "sender_name" -> senderName[0] = value;
                case "sender" -> {
                    if (tokens.size() > 1 && "name".equals(tokens.get(1))) {
                        senderName[0] = value;
                    } else if (tokens.size() > 1 && "handle".equals(tokens.get(1))) {
                        senderHandle[0] = value;
                    }
                }
                case "options" -> {
                    if (tokens.size() > 1 && "tags".equals(tokens.get(1))) {
                        tags.add(value);
                    }
                }
                default -> {
                    // ignore fields not relevant to display (e.g. options[archive], metadata[*])
                }
            }
        }

        return new ParsedFrontMessage(to, cc, bcc, subject[0], body[0], senderName[0], senderHandle[0], tags, attachments);
    }

    /** "options[tags][0]" -> ["options", "tags", "0"] */
    private List<String> tokenize(String fieldName) {
        List<String> tokens = new ArrayList<>();
        int firstBracket = fieldName.indexOf('[');
        if (firstBracket < 0) {
            tokens.add(fieldName);
            return tokens;
        }
        tokens.add(fieldName.substring(0, firstBracket));
        Matcher matcher = BRACKET_TOKEN.matcher(fieldName.substring(firstBracket));
        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }
        return tokens;
    }

    private String readText(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private List<String> readStringArray(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        if (node == null) {
            return values;
        }
        JsonNode value = node.get(field);
        if (value == null) {
            return values;
        }
        if (value.isArray()) {
            value.forEach(item -> values.add(item.asText()));
        } else if (!value.isNull()) {
            values.add(value.asText());
        }
        return values;
    }
}
