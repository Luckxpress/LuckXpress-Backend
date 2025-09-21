package com.luckxpress.data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
public class KYCDocument extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private LuckUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KYCStatus status = KYCStatus.PENDING;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Constructors
    public KYCDocument() {}

    public KYCDocument(LuckUser user, DocumentType documentType) {
        this.user = user;
        this.documentType = documentType;
        this.submittedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public LuckUser getUser() { return user; }
    public void setUser(LuckUser user) { this.user = user; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public KYCStatus getStatus() { return status; }
    public void setStatus(KYCStatus status) { this.status = status; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    // Enums
    public enum DocumentType {
        PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, UTILITY_BILL
    }

    public enum KYCStatus {
        PENDING, APPROVED, REJECTED
    }
}
