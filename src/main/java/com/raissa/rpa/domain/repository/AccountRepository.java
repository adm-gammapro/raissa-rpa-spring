package com.raissa.rpa.domain.repository;

import com.raissa.rpa.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    /**
     * Busca cuenta por key_access
     */
    Optional<Account> findByKeyAccess(String keyAccess);

    /**
     * Busca cuenta por numero de documento
     */
    Optional<Account> findByDocumentNumberAndActive(String documentNumber, Integer active);

    /**
     * Verifica si existe una cuenta con key_access
     */
    boolean existsByKeyAccess(String keyAccess);
}
