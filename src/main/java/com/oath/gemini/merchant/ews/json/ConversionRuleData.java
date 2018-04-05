package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.oath.gemini.merchant.ews.EWSConstant;
import lombok.Getter;
import lombok.Setter;
/**
 * @author pvardi on 1/27/2018
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ConversionRuleData {

    @JsonProperty(required = true)
    private Long id;

    @JsonProperty(required = true)
    private Long tagId;

    @JsonProperty(required = true)
    private String name;

    @JsonProperty(required = true)
    private EWSConstant.ConversionCategoryEnum conversionCategory;

    @JsonProperty(required = true)
    private JsonNode rule;

    private double conversionValue;

    private long cnt24h;

    private long cnt30d;

    private EWSConstant.ConversionRuleStatus status;

    @JsonProperty(required = true)
    private boolean defaultPixel;
}
