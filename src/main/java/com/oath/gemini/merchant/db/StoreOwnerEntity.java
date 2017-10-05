package com.oath.gemini.merchant.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "store_owner")
public class StoreOwnerEntity {
    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private int id;

    @Column(name = "store_acct_id", nullable = false, updatable = false)
    private int storeAcctId;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false)
    private String email;
    private String domain;
}
