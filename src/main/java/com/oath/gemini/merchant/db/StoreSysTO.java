package com.oath.gemini.merchant.db;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "store_sys")
public class StoreSysTO {
    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private int id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    private String description;
    private String domain;

    @Column(name = "cr_date", nullable = false)
    private Timestamp createdDate;

    @Column(name = "upd_date", nullable = false)
    private Timestamp updatedDate;
}
