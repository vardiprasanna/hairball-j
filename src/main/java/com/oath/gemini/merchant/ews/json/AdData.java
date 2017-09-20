package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.ews.EWSConstant.EditorialStatusEnum;
import com.oath.gemini.merchant.ews.EWSConstant.StatusEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @see https://developer.yahoo.com/gemini/guide/ad.html
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdData {
    @JsonProperty(required=true)
    private long id;

    @JsonProperty(required=true)
    private long adGroupId;

    @JsonProperty(required=true)
    private long campaignId;

    @JsonProperty(required=true)
    private long advertiserId;

    @JsonProperty(required=true)
    private String adGroupName;

    @JsonProperty(required=true)
    private StatusEnum status;

    /**
     * This is an optional field that allows you to name ads for your internal purposes. This value is not part of the
     * actual creative that is rendered. Character limit is 255. string Optional Optional
     */
    private String adName;

    /**
     * A list of JSON objects representing additional ad assets - for example, carousel image cards
     */
    private Object assets;

    /**
     * he description of the ad. Maximum limit is 150 characters.
     */
    private String description;

    /**
     * The user-friendly URL displayed to the user. Maximum limit is 35 characters.
     */
    private String displayUrl;

    /**
     * The URL of the image for the ad. Should be 627x627 pixels or larger (1:1 ratio - can display at various sizes
     * including 82x82). This field is required when creating a video ad.
     */
    private String imageUrl;

    /**
     * A URL to a 1200 x 627 high-resolution image that will be used in eligible native positions. This field is required
     * for INSTALL_APP or REENGAGE_APP campaigns. It is also required when creating a video ad.
     */
    private String imageUrlHQ;

    /**
     * A URL to a high-resolution image for an ad. The original image will be scaled to 180 x 180 pixels and can only be a
     * JPEG, PNG or GIF image.
     */
    private String imageUrlThumbnail;

    /**
     * The landing page URL associated with the ad. The landing page URL is the Web address that a user is sent to after
     * clicking on the ad. Maximum limit is 2048 characters. <br/>
     * 
     * In INSTALL_APP campaigns, use this field to input the tracking URL provided by your attribution vendor. Note that the
     * ${PIXEL_ID} and ${PIXEL_CONTEXT} macros need to be included in the tracking URL. Here is an example of a valid
     * landingUrl for an app install ad: https://hastrk0.com/5034488/${PIXEL_CONTEXT}${PIXEL_ID}. See the app install ad
     * example below for more details on passing in this field.
     */
    @JsonProperty(required=true)
    private String landingUrl;

    /**
     * A URL to a high-resolution image or a compressed HTML file for a mail sponsored ad. This can only run on VISIT_WEB
     * and CPC price type. Note that the image can only be a 2MB JPEG, PNG or GIF image. A compressed HTML file must be a
     * zip file and the maximum file size is 10MB.
     */
    private String mailAssetUrl;

    /**
     * The string shown against the sponsored by label in the ad. This field is not shown for ads that run on mobile search,
     * but is still a required input when creating any text ad. Maximum limit is 35 characters. For INSTALL_APP OR
     * REENGAGE_APP campaigns, we recommend that you put the app name in this field.
     */
    private String sponsoredBy;

    /**
     * The editorialStatus field is reserved for read-only system editorial review transitions. Note that this field is
     * read-only for Adds (creates) and Updates.
     */
    private EditorialStatusEnum editorialStatus;

    /**
     * The title for the ad. Maximum limit is 50 characters. string Required Optional
     */
    private String title;

    /**
     * The title for the ad. Maximum limit is 30 characters. When using this attribute title is also limited to 30
     * characters.
     */
    private String title2;

    /**
     * This field applies only to INSTALL_APP campaigns. If you have a Tumblr post you would like to associate with your ad,
     * you can provide a valid post URL using this field. Doing this will make your ad eligible to run on Tumblr.
     */
    private String contentUrl;

    /**
     * 
     * In INSTALL_APP, REENGAGE_APP or PROMOTE_BRAND campaigns you can provide a url for a video, making your campaign
     * eligible to serve in video-enabled native positions. The technical requirements for video are:
     * 
     * Format: .mp4 .m4v or .mov <br/>
     * Max File Size: 1GB <br/>
     * Min video length: 5sec <br/>
     * Max video length: 30sec (for INSTALL_APP or REENGAGE_APP campaigns) or 5min (for PROMOTE_BRAND campaigns) <br/>
     * Min video quality: 480p 300kbps. Recommended 500kbps.<br/>
     * Min audio quality: If the video has audio, then it must have 2 channel stereo 32kbps. Recommended: 64kbps. <br/>
     * 
     * Note that this is required for video ads running as PROMOTE_BRAND, APP_INSTALL or REENGAGE_APP campaigns.
     */
    private String videoPrimaryUrl;

    /**
     * Optional video caption file url to accompany the video url provided for video ads. Maximum url length is 1024
     * characters. The technical requirements for captions are:
     * 
     * Format: .vtt or .srt <br/>
     * Maximum File Size: 1MB <br/>
     * Maximum 3 lines of text per caption time entry/stamp in the file
     */
    private String videoCaptionUrl;

    /**
     * The impressionTrackingUrls object is used to set third-party view tracking for your ads. Please see below for more
     * information on how to set third-party view tracking. See app install ad example below for more details on passing in
     * this field.
     */
    private Object impressionTrackingUrls;

    /**
     * Call to action buttons used to help drive users to take specific actions. See the call to action section for a list
     * of available values.
     */
    private String callToActionText;
}
