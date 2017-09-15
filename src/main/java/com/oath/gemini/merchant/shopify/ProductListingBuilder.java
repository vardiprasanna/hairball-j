package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.shopify.data.ShopifyProductData;

/**
 * Build a product listing from calling Shopify prouduct services
 */
public class ProductListingBuilder {
    private ShopifyClientService svc;

    public ProductListingBuilder(ShopifyClientService svc) {
        this.svc = svc;
    }

    public void archetype() throws Exception {
        String result = svc.get(ShopifyEndpointEnum.SHOPIFY_PROD_COUNT);
        System.out.println(result);

        result = svc.get(ShopifyEndpointEnum.SHOPIFY_PROD_ALL);
        
        ShopifyProductData[] a = svc.get(ShopifyProductData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_ALL);
    }
}
