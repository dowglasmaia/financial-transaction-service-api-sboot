package com.dowglasmaia.financialtransaction.repository;

import com.dowglasmaia.financialtransaction.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update OutboxEvent e set e.publishedAt = :at, e.attempts = e.attempts + 1, e.lastError = null where e.id = :id")
    int markPublished(@Param("id") String id, @Param("at") OffsetDateTime at);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update OutboxEvent e set e.attempts = e.attempts + 1, e.lastError = :error where e.id = :id")
    int markFailed(@Param("id") String id, @Param("error") String error);
}
