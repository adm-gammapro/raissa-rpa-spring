package com.raissa.rpa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "params", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Params {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = false)
    private String provider;

    @Column(length = 50)
    private String grupo;

    @Column(length = 50)
    private String codigo;

    @Column(length = 500)
    private String valor;

    @Column
    private Short active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 25)
    private String createdBy;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 25)
    private String updatedBy;
}