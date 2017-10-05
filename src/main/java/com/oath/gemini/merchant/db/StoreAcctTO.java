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
@Table(name = "store_acct")
public class StoreAcctTO {
    public StoreAcctTO() {
        updatedDate = createdDate = new Timestamp(System.currentTimeMillis());
    }

    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private int id;

    @Column(name = "store_sys_id", nullable = false)
    private int storeSysId;

    @Column(name = "store_access_token", unique = true)
    private String storeAccessToken;

    @Column(name = "yahoo_access_token", unique = true)
    private String yahooAccessToken;

    @Column(name = "store_native_acct_id", unique = true)
    private String storeNativeAcctId;

    @Column(name = "gemini_native_acct_id", unique = true)
    private int geminiNativeAcctId;

    @Column(name = "cr_date", nullable = false)
    private Timestamp createdDate;

    @Column(name = "upd_date", nullable = false)
    private Timestamp updatedDate;
}
