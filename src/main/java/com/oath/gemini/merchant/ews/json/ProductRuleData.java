package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductRuleData {
    // The id of the Dot pixel placed on the DPA product page. Required
    @JsonProperty(value = "tagId", required = true)
    private Long pixelId;

    @JsonProperty(required = true)
    private Long advertiserId;

    // Use this to populate the product id in the pixel code. If not specified, it defaults to the product_id. Optional
    private String productIdExtractName = "product_id";

    // Use this to extract the event action. If not specified, it defaults to the ea parameter.
    private String eventActionExtractName = "ea";
}
