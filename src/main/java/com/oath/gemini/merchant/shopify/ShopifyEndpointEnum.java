package com.oath.gemini.merchant.shopify;

public enum ShopifyEndpointEnum {
    URL_FETCH_TOKEN("oauth/access_token"),

    URL_REQUEST_ACCESS("oauth/authorize"),

    URL_SHOP_APPS_PAGE("apps"),

    URL_PROD_COUNT("products/count.json"),

    URL_PROD_ALL("products.json"),

    URL_WRITE_SCRIPT_TAG("script_tags.json"),

    URL_SCRIPT_TAG_ALL("script_tags.json"),

    URL_SCRIPT_TAG_COUNT("script_tags/count.json"),

    PROD_FEED("https://${0}/collections/", "all.atom");
    private String url = "";

    ShopifyEndpointEnum(String subpath) {
        this("https://${0}/admin/", subpath);
    }

    ShopifyEndpointEnum(String path, String subpath) {
        url = path + subpath;
    }

    @Override
    public String toString() {
        return url;
    }
}
