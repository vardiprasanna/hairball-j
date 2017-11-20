package com.oath.gemini.merchant.shopify.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * @author tong 10/1/2017
 */
@JsonRootName(value = "webhooks")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopifyWebHooksData extends WebHook {
}
