package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @see https://developer.yahoo.com/gemini/guide/advertiser.html
 * @author tong on 10/1/2017
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvertiserData {
    @JsonProperty(required = true)
    private Long id;

    @JsonProperty(value = "advertiserName", required = true)
    private String name;

    @JsonProperty(required = true)
    private String timezone;

    @JsonProperty(required = true)
    private String currency;
}
