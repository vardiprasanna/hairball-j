package com.oath.gemini.merchant.shopify;

public enum ShopifyEndpointEnum {
    SHOPIFY_SHOP_INFO("shop.json"),

    SHOPIFY_FETCH_TOKEN("oauth/access_token"),

    SHOPIFY_REQUEST_ACCESS("oauth/authorize"),

    SHOPIFY_APPS_PAGE("apps"),

    SHOPIFY_PROD_COUNT("products/count.json"),

    SHOPIFY_PROD_ALL("products.json"),
    
    SHOPIFY_PROD_IMAGES("products/${0}/images.json"),
    
    SHOPIFY_PROD_VARIANTS("products/${0}/variants.json"),

    SHOPIFY_SCRIPT_TAG("script_tags.json"),

    SHOPIFY_SCRIPT_TAG_ALL("script_tags.json"),

    SHOPIFY_SCRIPT_TAG_OPS("script_tags/${0}.json"),
    
    SHOPIFY_PROD_LISTING("product_listings.json"),

    SHOPIFY_PROD_FEED("https://${0}/collections/", "all.atom");
    private String url = "";

    ShopifyEndpointEnum(String subpath) {
        this("https://${shop}/admin/", subpath);
    }

    ShopifyEndpointEnum(String path, String subpath) {
        url = path + subpath;
    }

    @Override
    public String toString() {
        return url;
    }
}
