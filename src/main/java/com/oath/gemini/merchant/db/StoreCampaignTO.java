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
@Table(name = "store_campaign")
public class StoreCampaignTO {
    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private int id;

    @Column(name = "store_acct_id", nullable = false, updatable = false)
    private int storeAcctId;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "adv_id", nullable = false)
    private Long advId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "adgroup_id")
    private Long adgroupId;

    @Column(name = "pixel_id", nullable = false)
    private int pixelId;

    private int status;
}
