package com.oath.gemini.merchant.db;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@Getter
@Setter
@Entity
@Table(name = "store_sys")
@Where(clause = "isDeleted !=true")
public class StoreSysEntity {
    public StoreSysEntity() {
        updatedDate = createdDate = new Timestamp(System.currentTimeMillis());
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    private String description;
    private String domain;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "cr_date", nullable = false)
    private Timestamp createdDate;

    @Column(name = "upd_date", nullable = false)
    private Timestamp updatedDate;
}
