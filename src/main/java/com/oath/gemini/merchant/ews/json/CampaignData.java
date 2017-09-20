package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.ews.EWSConstant.AdvancedGeoNegEnum;
import com.oath.gemini.merchant.ews.EWSConstant.AdvancedGeoPosEnum;
import com.oath.gemini.merchant.ews.EWSConstant.ChannelEnum;
import com.oath.gemini.merchant.ews.EWSConstant.ObjectiveEnum;
import com.oath.gemini.merchant.ews.EWSConstant.StatusEnum;
import com.oath.gemini.merchant.ews.EWSConstant.SubChannelEnum;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * @see https://developer.yahoo.com/gemini/guide/campaigns.html
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignData {
    @JsonProperty(required=true)
    private long id;

    @JsonProperty(required=true)
    private long advertiserId;

    /**
     * The status of the campaign. Valid values are:
     * 
     * ACTIVE PAUSED DELETED Campaigns can be created in an ACTIVE state. If a campaign is in a PAUSED state, specify valid
     * start and end dates at the ad group level in order to activate it.
     */
    @JsonProperty(required=true)
    private StatusEnum status;

    /**
     * The read-only effective status of the campaign based on the direct campaign status, accounts funds, campaign budget,
     * and summarized ad group flight dates. Valid values are:
     * 
     * ACTIVE PAUSED DELETED ENDED LIFETIME_BUDGET_SPENT AWAITING_FUNDS AWAITING_START_DATE
     */
    @JsonProperty(required=true)
    private String effectiveStatus;

    /**
     * The budget for your campaign in the currency provided in your billing settings. Using the budgetType attribute, you
     * can either set a daily spend cap or provide a budget for the entire duration of the campaign. Note that budget should
     * be set so that it is at least 50 times your desired bid for Native campaigns and 1 times your desired bid for Search
     * campaigns.
     */
    @JsonProperty(required=true)
    private BigDecimal budget;

    /**
     * Type of budget. Value can be:
     * 
     * LIFETIME - the budget is for the entire duration of the campaign. DAILY - the budget is the daily spend amount. Daily
     * spend will be capped at the budget value.
     */
    @JsonProperty(required=true)
    private String budgetType;

    /**
     * The name of the campaign. Maximum limit is 100 characters.
     */
    @JsonProperty(required=true)
    private String campaignName;

    /**
     * The supply channel on which the campaign will run. The available options are:
     * 
     * NATIVE - will run in native positions on Yahoo! content streams SEARCH - will run on mobile search supply
     * SEARCH_AND_NATIVE - will run on both mobile search and content streams supply If channel is not set, default value
     * will be SEARCH_AND_NATIVE. Please also note that for your ads to actually serve on the channels you have set, you
     * will also need to set a bid for this channel.
     */
    private ChannelEnum channel;

    /**
     * The supply channel on which the campaign will run. The sub channel is only applicable when channel is SEARCH. The
     * available options are:
     * 
     * SRN_AND_SEARCH SRN_ONLY DEFAULT - to remove
     */
    private SubChannelEnum subChannel;

    /**
     * Bid modifiers allow you to increase or decrease your campaign bids. Allowed value: 0.1 to 2.
     * 
     * Note: Required if subChannel is set. Default 0.3
     */
    private Double subChannelModifier;

    /**
     * The language of the targeted audience. By default, this is set to en (for english). This can be set at the time of
     * campaign creation and cannot be modified. For the list of supported languages, refer to the data dictionary section.
     */
    private String language;

    /**
     * The objective of the campaign. Value can be:
     * 
     * VISIT_WEB - This is the default value and it should be used if your goal is to generate traffic to your webpages.
     * This objective supports CPC pricing type for both mobile SEARCH and NATIVE ad campaigns. VISIT_OFFER - Use this
     * objective for dynamic product ads campaigns. Note that the objective supports the CPC pricing type and NATIVE
     * campaigns only. PROMOTE_BRAND - Use this objective if your goal is to increase brand awareness. This objective
     * supports CPM or CPV pricing type and NATIVE campaigns only. INSTALL_APP - Use this objective if your goal is to
     * generate app downloads. This objective supports the CPC pricing type and NATIVE campaigns only. For more details, see
     * the create an app install campaign section. Objective can only be set at the time of campaign creation and cannot be
     * modified.
     * 
     * REENGAGE_APP - Use this objective if your goal is to generate app re-engagements. This objective supports the CPC
     * pricing type and NATIVE campaigns only. For more details, see the create a re-engage app campaign section.
     */
    private ObjectiveEnum objective;

    /**
     * Applies only to SEARCH campaigns. Determines whether your campaign will run on the Yahoo partner network. This field
     * accepts TRUE or FALSE and defaults to TRUE. For NATIVE campaigns this field must be set to TRUE.
     */
    private String isPartnerNetwork;

    /**
     * This field is required only for INSTALL_APP and REENGAGE_APP campaigns. It should be used to pass the App Store or
     * Google Play Store app url, and cannot be updated once set. Note that for INSTALL_APP, tracking parameters and
     * redirects should not be part of the defaultLandingUrl. These should be provided at the ad level through the ad
     * landingUrl.
     */
    private String defaultLandingUrl;

    /**
     * This field is required only for INSTALL_APP campaigns. Use this field to specify the vendor you are using in order to
     * track app install conversions. For a list of approved third-party tracking partners, refer to the data dictionary
     * (https://api.gemini.yahoo.com/v2/rest/dictionary/tracking_partners/).
     * 
     * Note: Required (only for INSTALL_APP campaigns)
     */
    private String trackingPartner;

    /**
     * This is an optional field for INSTALL_APP and REENGAGE_APP campaigns. It can be used to specify the desired locale of
     * the app store, and will default to “en-us” if not specified. For a list of available locales, refer to the data
     * dictionary.
     */
    private String appLocale;

    /**
     * Applies only to SEARCH campaigns. For the locations you target, the recommended and default value is DEFAULT, which
     * means you will reach people either physically in your targeted locations or who have expressed interest in these
     * locations. LOCATION_OF_PRESENCE means reaching only people physically in the targeted location, and
     * LOCATION_OF_INTEREST means targeting only people who have expressed interest in the location.
     */
    private AdvancedGeoPosEnum advancedGeoPos;

    /**
     * Applies only to SEARCH campaigns. Similar to advancedGeoPos but applies only to locations you exclude. Valid values
     * are DEFAULT (the default) and LOCATION_OF_PRESENCE.
     */
    private AdvancedGeoNegEnum advancedGeoNeg;

    /**
     * The conversion rule IDs to associate to this campaign. Must be valid IDs of conversion rules created in an account.
     * To remove IDs, simply pass an empty array.
     */
    private long[] conversionRuleIds;

    /**
     * The campaign conversion configuration. Details are provided below.
     */
    private Object conversionRuleConfig;
}
