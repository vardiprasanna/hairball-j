package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong 10/1/2017
 */
@JsonTypeName(value = "storefront_access_token")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopifyStoreFrontTokenData extends StoreFrontToken {
}

@Getter
@Setter
class StoreFrontToken {
    @JsonProperty(required = true)
    private Long id;

    private String title;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("access_scope")
    private String scope;
}