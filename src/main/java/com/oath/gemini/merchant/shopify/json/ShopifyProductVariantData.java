package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Getter;
import lombok.Setter;

@JsonRootName("variants")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopifyProductVariantData {

    @JsonProperty(required = true)
    private String id;

    @JsonProperty(required = true)
    private String product_id;

    private String title;

    private String barcode;

    private String sku;

    private String image_id;

    private Integer inventory_quantity;

    private float price;
}
