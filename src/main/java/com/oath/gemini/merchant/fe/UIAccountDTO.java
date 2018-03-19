package com.oath.gemini.merchant.fe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.db.StoreAcctEntity;
import com.oath.gemini.merchant.ews.EWSConstant;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 1/1/2018
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UIAccountDTO {
    public UIAccountDTO() {
    }

    public UIAccountDTO(StoreAcctEntity acctEntity) {
        if (acctEntity != null) {
            this.name = acctEntity.getName();
            this.geminiNativeAcctId = acctEntity.getGeminiNativeAcctId();
            this.storeAccessToken = acctEntity.getStoreAccessToken();
            this.yahooAccessToken = acctEntity.getYahooAccessToken();
        }
    }

    @JsonProperty(value = "adv_status")
    private EWSConstant.StatusEnum status;

    @JsonProperty(value = "adv_name")
    private String name;

    @JsonProperty(value = "adv_id")
    private Integer geminiNativeAcctId;

    @JsonProperty(value = "cmp_id")
    private Long geminiNativeCampaignId;

    @JsonProperty(value = "store_access_token")
    private String storeAccessToken;

    @JsonProperty(value = "yahoo_access_token")
    private String yahooAccessToken;

    @JsonProperty(value = "billing_valid", required = true)
    private Boolean isBillingValid = false;

    @JsonProperty(value = "store_token_valid", required = true)
    private Boolean isStoreTokenValid = false;

    @JsonProperty(value = "yahoo_token_valid", required = true)
    private Boolean isYahooTokenValid = false;

    @JsonProperty(value = "store_auth_uri")
    private String storeAuthUrl;

    @JsonProperty(value = "yahoo_auth_uri")
    private String yahooAuthUrl;

    // shop domain - e.g., dpa-bridge.myshopify.com
    private String shop;
}
