package com.raissa.rpa.domain.repository;

import com.raissa.rpa.domain.entity.RequestInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestInformationRepository extends JpaRepository<RequestInformation, Long> {
    /**
     * Busca registros por session_id
     */
    List<RequestInformation> findBySessionId(Long sessionId);

    /**
     * Busca registros por código de respuesta
     */
    List<RequestInformation> findByResponseStatus(String responseStatus);

    /**
     * Cuenta registros por session_id
     */
    @Query("SELECT COUNT(r) FROM RequestInformation r WHERE r.session.id = :sessionId")
    Long countBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Busca último registro por session_id
     */
    @Query("SELECT r FROM RequestInformation r WHERE r.session.id = :sessionId ORDER BY r.createdAt DESC LIMIT 1")
    Optional<RequestInformation> findLatestBySessionId(@Param("sessionId") Long sessionId);
}
