package com.oath.gemini.merchant.db;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "store_acct")
public class StoreAcctEntity {
    public StoreAcctEntity() {
        updatedDate = createdDate = new Timestamp(System.currentTimeMillis());
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String domain;

    @Column(name = "store_sys_id", nullable = false)
    private Integer storeSysId;

    @Column(name = "store_access_token", unique = true)
    private String storeAccessToken;

    @Column(name = "yahoo_access_token", unique = true)
    private String yahooAccessToken;

    @Column(name = "store_native_acct_id", unique = true)
    private String storeNativeAcctId;

    @Column(name = "gemini_native_acct_id", unique = true)
    private Integer geminiNativeAcctId;

    @Column(name = "pixel_id", nullable = false)
    private Integer pixelId;

    @Column(name = "cr_date", nullable = false)
    private Timestamp createdDate;

    @Column(name = "upd_date", nullable = false)
    private Timestamp updatedDate;
}
