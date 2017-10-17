package com.oath.gemini.merchant.db;

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
@Table(name = "store_owner")
@Where(clause="is_deleted = 0")
public class StoreOwnerEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer id;

    @Column(name = "store_acct_id", nullable = false, updatable = false)
    private Integer storeAcctId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private String email;
    private String domain;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
