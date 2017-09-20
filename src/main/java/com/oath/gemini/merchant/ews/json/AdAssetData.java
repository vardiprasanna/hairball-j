package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.ews.EWSConstant;
import com.oath.gemini.merchant.ews.EWSConstant.AdAssetTypeEnum;
import com.oath.gemini.merchant.ews.EWSConstant.StatusEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @see https://developer.yahoo.com/gemini/guide/ad.html#ad-assets
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdAssetData {
    @JsonProperty(required=true)
    private long id;

    @JsonProperty(required=true)
    private EWSConstant.StatusEnum status;

    /**
     * Title text used for the ad asset. Maximum limit is 50 characters.
     * 
     * Note: required for carousel image assets
     */
    private String title;

    /**
     * Description text displayed when your ad asset appears in content streams or in the other eligible native ad
     * positions. Maximum 150 characters.
     */
    @JsonProperty(required=true)
    private String description;

    /**
     * A valid URL for a thumbnail image that will be displayed when your ad appears in content streams or in the other
     * eligible native ad positions. The ideal image size is 627x627. The maximum size is 2MB.
     * 
     * Note: required for carousel image assets
     */
    private String imageUrl;

    /**
     * A valid URL to a high resolution image that will be used in eligible native ad positions. The ideal image size is
     * 1200x627; smaller images will not be accepted. Larger images with either the same aspect ratio or with a height and
     * width that are off the ideal dimensions by up to a combined 10% will be accepted as well as will be automatically
     * cropped at the center. The maximum file size is 2MB.
     * 
     * Note: required for carousel image assets
     */
    private String imageUrlHQ;

    /**
     * Index ordering of assets. For carousel image assets (type=MULTI_IMAGE) the values must be unique for every active
     * asset and must be between 0 and 4. Minimum 3 assets are required for carousel image ad.
     * 
     * Note: required for carousel image assets
     */
    private Integer index;

    /**
     * Call to action buttons used to help drive users to take specific actions.
     */
    private String callToActionText;

    /**
     * The type of ad asset. Valid values are: MULTI_IMAGE (for carousel ad image assets)
     */
    @JsonProperty(required=true)
    private EWSConstant.AdAssetTypeEnum type;

    /**
     * The landing URL associated with the ad. The landing URL is the web address that a user is sent to after clicking on
     * the ad. Maximum limit is 2048 characters. The landing URL should include the tracking params provided by the tracking
     * partner that was specified at the campaign level for app installs.
     */
    private String landingUrl;

}
