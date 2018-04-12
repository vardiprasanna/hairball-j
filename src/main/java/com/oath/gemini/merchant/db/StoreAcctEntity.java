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
@Table(name = "store_acct")
@Where(clause="is_deleted is null or is_deleted != 1")
public class StoreAcctEntity extends StoreBaseEntity {
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

    @Column(name = "conversion_rule_id")
    private Long conversionRuleId;
}
