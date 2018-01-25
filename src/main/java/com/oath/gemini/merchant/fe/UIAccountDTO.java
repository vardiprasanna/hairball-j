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
    public UIAccountDTO(StoreAcctEntity acctEntity) {
        this.name = acctEntity.getName();
        this.geminiNativeAcctId = acctEntity.getGeminiNativeAcctId();
        this.storeAccessToken = acctEntity.getStoreAccessToken();
        this.yahooAccessToken = acctEntity.getYahooAccessToken();
    }

    @JsonProperty(value = "adv_status", required = true)
    private EWSConstant.StatusEnum status = EWSConstant.StatusEnum.ACTIVE;

    @JsonProperty(value = "adv_name", required = true)
    private String name;

    @JsonProperty(value = "adv_id", required = true)
    private Integer geminiNativeAcctId;

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
}
