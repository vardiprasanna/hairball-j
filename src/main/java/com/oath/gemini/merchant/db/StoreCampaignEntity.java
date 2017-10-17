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
@Table(name = "store_campaign")
@Where(clause="is_deleted = 0")
public class StoreCampaignEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer id;

    @Column(name = "store_acct_id", nullable = false, updatable = false)
    private Integer storeAcctId;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "adv_id", nullable = false)
    private Long advId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "adgroup_id")
    private Long adgroupId;

    @Column(name = "product_feed_id")
    private Long productFeedId;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    private Integer status;
}
