package com.oath.gemini.merchant.fe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.json.CampaignData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UICampaignDTO {
    public UICampaignDTO() {
    }

    public UICampaignDTO(CampaignData cmpData) {
        this.status = cmpData.getStatus();
        this.campaignName = cmpData.getCampaignName();
        this.advId = cmpData.getAdvertiserId();
        this.campaignId = cmpData.getId();
        this.budget = (cmpData.getBudget() != null ? cmpData.getBudget().floatValue() : 0f);
    }

    public UICampaignDTO(StoreCampaignEntity cmpEntity) {
        this.status = cmpEntity.getStatus();
        this.advId = cmpEntity.getAdvId();
        this.campaignId = cmpEntity.getCampaignId();
        this.budget = cmpEntity.getBudget();
        this.price = cmpEntity.getPrice();
        this.startDateInMilli = cmpEntity.getStartDate().getTime();
        this.endDateInMilli = cmpEntity.getEndDate().getTime();
    }

    private EWSConstant.StatusEnum status;
    private String advName;
    private String campaignName;
    private long advId;
    private long campaignId;
    private float budget;
    private float price;
    private Long startDateInMilli;
    private Long endDateInMilli;
}
