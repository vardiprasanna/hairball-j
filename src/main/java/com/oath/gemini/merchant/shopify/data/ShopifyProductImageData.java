package com.oath.gemini.merchant.shopify.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Getter;
import lombok.Setter;

@JsonRootName("images")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopifyProductImageData {

    @JsonProperty(required = true)
    private String id;

    @JsonProperty(required = true)
    private String product_id;

    @JsonProperty(required = true)
    private String src;

    @JsonProperty(required = true)
    private Integer width;

    @JsonProperty(required = true)
    private Integer height;

    String[] variant_ids;
}
