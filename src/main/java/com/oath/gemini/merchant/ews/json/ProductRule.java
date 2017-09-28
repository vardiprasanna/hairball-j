package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductRule {
    @JsonProperty(value = "tagId", required = true)
    // The id of the Dot pixel placed on the DPA product page. Required
    private long pixelId;

    @JsonProperty(required = true)
    private long advertiserId;

    // Use this to populate the product id in the pixel code. If not specified, it defaults to the product_id. Optional
    private String productIdExtractName;

    // Use this to extract the event action. If not specified, it defaults to the ea parameter.
    private String eventActionExtractName;
}
