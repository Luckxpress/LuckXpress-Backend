package com.luckxpress.data.repository;

import com.luckxpress.data.entity.KYCDocument;
import com.luckxpress.data.entity.LuckUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KYCDocumentRepository extends JpaRepository<KYCDocument, Long> {

    List<KYCDocument> findByUser(LuckUser user);

    @Query("SELECT d FROM KYCDocument d WHERE d.status = :status")
    Page<KYCDocument> findByStatus(@Param("status") KYCDocument.KYCStatus status, Pageable pageable);

    @Query("SELECT d FROM KYCDocument d WHERE " +
           "(:userId IS NULL OR d.user.id = :userId) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:docType IS NULL OR d.documentType = :docType)")
    Page<KYCDocument> findDocumentsWithFilters(
        @Param("userId") Long userId,
        @Param("status") KYCDocument.KYCStatus status,
        @Param("docType") KYCDocument.DocumentType docType,
        Pageable pageable
    );

    long countByStatus(KYCDocument.KYCStatus status);
}
