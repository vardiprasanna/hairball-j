package com.oath.gemini.merchant.ews;

public enum EWSEndpointEnum {
    ADVERTISER("advertiser"),

    CAMPAIGN_OPS("campaign"), CAMPAIGN_BY_ADVERTISER("campaign/?advertiserId=${0}"),

    ADGROUP_OPS("adgroup"), ADGROUP_BY_ID("adgroup/${0}"), ADGROUP_BY_CAMPAIGN("adgroup?campaignId=${0}"), ADGROUP_BY_ADVERTISER(
            "adgroup?advertiserId=${0}");

    EWSEndpointEnum(String path) {
        url = BASE_URL + path;
    }

    @Override
    public String toString() {
        return url;
    }

    private String url = "";
    private final static String BASE_URL = "https://api.gemini.yahoo.com/v3/rest/";
}
