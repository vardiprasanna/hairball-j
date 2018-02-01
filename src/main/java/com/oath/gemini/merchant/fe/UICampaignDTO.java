package com.oath.gemini.merchant.fe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.db.StoreCampaignEntity;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.json.CampaignData;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 1/1/2018
 */
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
    
    @JsonProperty(value = "adv_name")
    private String advName;

    @JsonProperty(value = "cmp_name")
    private String campaignName;
    
    @JsonProperty(value = "adv_id")
    private long advId;
    
    @JsonProperty(value = "cmp_id")
    private long campaignId;
    
    private float budget;
    private float price;
    
    @JsonProperty(value = "start_date")
    private Long startDateInMilli;
    
    @JsonProperty(value = "end_date")
    private Long endDateInMilli;
}
