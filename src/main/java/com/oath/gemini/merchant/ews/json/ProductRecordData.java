package com.oath.gemini.merchant.ews.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oath.gemini.merchant.ews.EWSConstant.PrdAgeGroupEnum;
import com.oath.gemini.merchant.ews.EWSConstant.PrdAvailabilityEnum;
import com.oath.gemini.merchant.ews.EWSConstant.PrdConditionEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductRecordData {
    /**
     * Unique id to identify the product. The product sets, user recommendation and dot pixel custom events should use this
     * id. All products with duplicate ids will be ignored.
     */
    @JsonProperty(required = true)
    private String id; // max 100

    /**
     * The title of the product. This field will be used in the ad presentation. Title displayed is 20-50 characters.
     */
    @JsonProperty(required = true)
    private String title; // max 100

    /**
     * A short description of the product. This field will be used in the ad presentation. Description displayed is 20-150
     * characters.
     */
    @JsonProperty(required = true)
    private String description; // max 5000

    /**
     * Link to an image of the product. The image provided will be mapped to these formats for native ads: large 627x627px
     * and HQ 1200x627px.
     */
    @JsonProperty(required = true)
    private String image_link;

    /**
     * Landing page of the merchant when the ad is clicked.
     */
    @JsonProperty(required = true)
    private String link;

    /**
     * Defines product availability. Accepted values are: <br/>
     * in stock - product is in stock and can be shipped immediately. <br/>
     * out of stock - product is currently unavailable. <br/>
     * preorder- product will be available in future. <br/>
     * available for order - Product can be shipped in 1-2 weeks. <br/>
     * out of stock products are not eligible for serving and may be removed from the system after 3 months
     */
    @JsonProperty(required = true)
    private PrdAvailabilityEnum availability = PrdAvailabilityEnum.IN_STOCK;

    /**
     * The condition of the product. Accepted values are new, refurbished, or used.
     */
    @JsonProperty(required = true)
    private PrdConditionEnum condition = PrdConditionEnum.NEW;

    /**
     * The cost of the product and currency. Currency should be specified as the ISO 4217 currency code e.g. 9.99 USD. This
     * field will be used in ad presentation along with title as: title - price. The currency symbol will be used. E.g.
     * Bead-Fringe Suede Ankle Boot - $1200.
     */
    @JsonProperty(required = true)
    private String price;

    /**
     * gtin - The Global Trade Item Number (GTINs) can include UPC, EAN, JAN, and ISBN. <br/>
     * mpn - The number which uniquely identifies the product to its manufacturer. <br/>
     * brand - The name of the brand.
     * 
     * Note: one of gtin/mpn/brand is required
     */
    private String gtin; // max 70
    private String mpn; // max 70
    private String brand; // max 70

    /**
     * Comma separated multiple (max 10) additional image urls can be provided.
     */
    private String additional_image_link; // max 2000

    /**
     * The age group the product is meant for. Accepted values are newborn, infant, toddler, kids, and adult.
     */
    private PrdAgeGroupEnum age_group;

    /**
     * The color of the product.
     */
    private String color; // max 100

    /**
     * The expiration date of the product. An expired product is not eligible for serving.
     */
    private String expiration_date; // ISO‑8601 (YYYY‑MM‑DD)

    /**
     * Acceptable values are male, female, and unisex
     */
    private String gender;

    /**
     * Similar products can share same item group id
     */
    private String item_group_id;

    /**
     * Predefined values from Google’s product taxonomy. For example, Apparel & Accessories > Clothing > Dresses.
     */
    private String google_product_category; // max 250

    /**
     * Material or fabric of the product.
     */
    private String material; // max 200

    /**
     * The pattern or graphic print featured on a product.
     */
    private String pattern; // max 100

    /**
     * The retailer-defined category of the product as a string. Examples : TSV format: Home & Garden > Kitchen & Dining >
     * Appliances > Refrigerators
     */
    private String product_type; // max 750

    /**
     * The discounted price if the product is on sale. Currency should be specified as the ISO 4217 currency code. Specified
     * as 9.99 USD
     */
    private String sale_price; // max 750

    /**
     * The start and end date/time of the sale, separated by a slash. e.g., 2014-11-01T12:00-0300/2014-12-01T00:00-0300
     */
    private String sale_price_effective_date; // ISO‑8601 (YYYY‑MM‑DD)

    private String shipping;

    private String shipping_weight;

    private String shipping_size;

    /**
     * Can contain additional information about the item
     */
    private String custom_label_0; // max 100

    private String custom_label_1; // max 100

    private String custom_label_2; // max 100

    private String custom_label_3; // max 100

    private String custom_label_4; // max 100
}
