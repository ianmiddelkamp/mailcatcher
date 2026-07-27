package dev.middelkamp.mailcatcher.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class CapturedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String endpointLabel;

    private String senderName;
    private String senderHandle;

    private String toAddresses;
    private String ccAddresses;
    private String bccAddresses;

    private String subject;

    @Lob
    @Column(length = 100_000)
    private String body;

    private String tags;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CapturedAttachment> attachments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getEndpointLabel() {
        return endpointLabel;
    }

    public void setEndpointLabel(String endpointLabel) {
        this.endpointLabel = endpointLabel;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderHandle() {
        return senderHandle;
    }

    public void setSenderHandle(String senderHandle) {
        this.senderHandle = senderHandle;
    }

    public String getToAddresses() {
        return toAddresses;
    }

    public void setToAddresses(String toAddresses) {
        this.toAddresses = toAddresses;
    }

    public String getCcAddresses() {
        return ccAddresses;
    }

    public void setCcAddresses(String ccAddresses) {
        this.ccAddresses = ccAddresses;
    }

    public String getBccAddresses() {
        return bccAddresses;
    }

    public void setBccAddresses(String bccAddresses) {
        this.bccAddresses = bccAddresses;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<String> getTagList() {
        return tags == null || tags.isBlank() ? List.of() : List.of(tags.split(", "));
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<CapturedAttachment> getAttachments() {
        return attachments;
    }
}
