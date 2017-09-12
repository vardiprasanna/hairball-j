package com.oath.gemini.merchant.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.ews.EWSConstant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSetData {
    @JsonProperty(required = true)
    private long id;

    @JsonProperty(required = true)
    private long advertiserId;

    @JsonProperty(required = true)
    private EWSConstant.StatusEnum status;

    @JsonProperty(required = true)
    private String filter;
}
