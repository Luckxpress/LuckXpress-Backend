package com.luckxpress.controller;

import com.luckxpress.data.entity.KYCDocument;
import com.luckxpress.data.entity.LuckUser;
import com.luckxpress.data.repository.KYCDocumentRepository;
import com.luckxpress.data.repository.LuckUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/kyc")
@CrossOrigin(origins = "http://localhost:3000")
public class KYCController {

    @Autowired
    private KYCDocumentRepository kycRepository;

    @Autowired
    private LuckUserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getKycDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String docType) {

        Sort.Direction direction = order.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        KYCDocument.KYCStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = KYCDocument.KYCStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        KYCDocument.DocumentType docTypeEnum = null;
        if (docType != null && !docType.isEmpty()) {
            try {
                docTypeEnum = KYCDocument.DocumentType.valueOf(docType.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<KYCDocument> kycPage = kycRepository.findDocumentsWithFilters(userId, statusEnum, docTypeEnum, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("data", kycPage.getContent());
        response.put("total", kycPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getKycDocument(@PathVariable Long id) {
        return kycRepository.findById(id)
                .map(doc -> ResponseEntity.ok(Map.of("data", doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateKycStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<KYCDocument> kycDocOptional = kycRepository.findById(id);
        if (kycDocOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        KYCDocument kycDoc = kycDocOptional.get();
        String newStatus = payload.get("status");
        String reviewedBy = payload.getOrDefault("reviewedBy", "admin");

        try {
            KYCDocument.KYCStatus statusEnum = KYCDocument.KYCStatus.valueOf(newStatus.toUpperCase());
            kycDoc.setStatus(statusEnum);
            kycDoc.setReviewedAt(LocalDateTime.now());
            kycDoc.setReviewedBy(reviewedBy);

            if (statusEnum == KYCDocument.KYCStatus.REJECTED) {
                kycDoc.setRejectionReason(payload.get("rejectionReason"));
            }
            
            // Also update the user's KYC status
            LuckUser user = kycDoc.getUser();
            user.setKycStatus(LuckUser.KYCStatus.valueOf(statusEnum.name()));
            userRepository.save(user);

            KYCDocument updatedDoc = kycRepository.save(kycDoc);
            return ResponseEntity.ok(Map.of("data", updatedDoc));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
        }
    }

    @PostMapping("/init-sample-data")
    public ResponseEntity<String> initSampleKycData() {
        List<LuckUser> users = userRepository.findAll();
        if (users.isEmpty() || kycRepository.count() > 0) {
            return ResponseEntity.ok("KYC sample data already exists or no users found.");
        }

        // Create PENDING KYC doc for first user
        LuckUser user1 = users.get(0);
        KYCDocument doc1 = new KYCDocument(user1, KYCDocument.DocumentType.PASSPORT);
        doc1.setFilePath("/documents/sample_passport.jpg");
        kycRepository.save(doc1);

        // Create APPROVED KYC doc for second user
        if (users.size() > 1) {
            LuckUser user2 = users.get(1);
            KYCDocument doc2 = new KYCDocument(user2, KYCDocument.DocumentType.DRIVERS_LICENSE);
            doc2.setStatus(KYCDocument.KYCStatus.APPROVED);
            doc2.setReviewedAt(LocalDateTime.now().minusDays(1));
            doc2.setReviewedBy("auto-approver");
            doc2.setFilePath("/documents/sample_license.jpg");
            kycRepository.save(doc2);
            user2.setKycStatus(LuckUser.KYCStatus.APPROVED);
            userRepository.save(user2);
        }

        return ResponseEntity.ok("Sample KYC data created.");
    }
}
