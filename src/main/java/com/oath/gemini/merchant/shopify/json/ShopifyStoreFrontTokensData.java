package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonRootName(value = "storefront_access_tokens")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopifyStoreFrontTokensData extends StoreFrontToken {
}
