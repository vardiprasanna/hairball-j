package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @see https://developer.yahoo.com/gemini/guide/adgroup.html
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdGroupData {
    @JsonProperty(required = true)
    private long id;

    @JsonProperty(required = true)
    private long campaignId;

    @JsonProperty(required = true)
    private long advertiserId;

    @JsonProperty(required = true)
    private String adGroupName;

    @JsonProperty(required = true)
    private EWSConstant.StatusEnum status;

    /**
     * The id of the associate product set.
     */
    private long productSetId;

    /**
     * The start date string value of the ad group (in YYYY-mm-dd format) in the timezone of the account. The time will
     * automatically get set to 00:00:00.000
     */
    @JsonProperty(required = true)
    private String startDateStr;

    /**
     * The end date string value of the ad group (in YYYY-mm-dd format) in the timezone of the account. The time will
     * automatically get set to 23:59:59.000
     */
    @JsonProperty(required = true)
    private String endDateStr;

    /**
     * A list of bids - the value can be one or more bids. Please refer to the bidSet Object fields for more information.
     * Note that bids should fulfill a budget-to-bid ratio of 50:1 for Native campaigns and 1:1 for Search campaigns.
     */
    @JsonProperty(required = true)
    private MultiBids bidSet = new MultiBids();

    private EWSConstant.AdvancedGeoPosEnum advancedGeoPos;
    private EWSConstant.AdvancedGeoNegEnum advancedGeoNeg;

    /**
     * Applies only to native campaigns with either VISIT_WEB or INSTALL_APP objectives. Available values:
     *
     * OPT_CONVERSION - means that Gemini will dynamically modify your bid in order to optimize for conversions. bidSet is
     * still required and will define the initial bid that Gemini will gradually optimize. You also need to have at least
     * one conversion rule set up in order to leverage this strategy. Note that you need to have an ecpaGoal in order to set
     * OPT_CONVERSION. For install app campaigns, the bidding strategy will be set to OPT_CONVERSION.
     * 
     * DEFAULT - the default bidding strategy, meaning Gemini will optimize per your selected price type (deliver clicks for
     * CPC ads groups, impressions for CPM ads). See an example in the Create a new ad group section.
     */
    private EWSConstant.BiddingStrategyEnum biddingStrategy;

    /**
     * This is the value you place on the conversion. Refer to Data Dictionary to look up currency by type. Note that
     * decreasing your ecpaGoal will typically lower the delivery of your campaign. If the bidding strategy is
     * OPT_CONVERSION, the ecpaGoal is required. For install app campaigns, the ecpaGoal is also required.
     */
    @JsonProperty(required = true)
    private Float ecpaGoal;
}

@Getter
@Setter
class MultiBids {
    @JsonProperty(required = true)
    private BidSetData[] bids;
}
