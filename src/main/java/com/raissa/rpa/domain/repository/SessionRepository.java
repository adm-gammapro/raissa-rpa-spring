package com.raissa.rpa.domain.repository;

import com.raissa.rpa.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    /**
     * Busca sesión por transactionId
     */
    Optional<Session> findByTransactionId(String transactionId);

    /**
     * Busca sesiones activas por account_id
     */
    @Query("SELECT s FROM Session s WHERE s.account.id = :accountId AND s.active = 1 AND (s.expires IS NULL OR s.expires > :now)")
    List<Session> findActiveSessionsByAccount(@Param("accountId") Long accountId,
                                              @Param("now") LocalDateTime now);

    /**
     * Busca sesión activa por token
     */
    @Query("SELECT s FROM Session s WHERE s.token = :token AND s.active = 1 AND (s.expires IS NULL OR s.expires > :now)")
    Optional<Session> findActiveSessionByToken(@Param("token") String token,
                                               @Param("now") LocalDateTime now);

    /**
     * Desactiva todas las sesiones de una cuenta
     */
    @Query("UPDATE Session s SET s.active = 0, s.disabledAt = :now WHERE s.account.id = :accountId AND s.active = 1")
    void deactivateAllSessionsByAccount(@Param("accountId") Long accountId,
                                        @Param("now") LocalDateTime now);
}
