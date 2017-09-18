package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.feed.ProductConstant;
import com.oath.gemini.merchant.feed.ProductRecordData;
import com.oath.gemini.merchant.shopify.data.ShopifyProductData;
import com.oath.gemini.merchant.shopify.data.ShopifyProductImageData;
import com.oath.gemini.merchant.shopify.data.ShopifyProductVariantData;
import java.io.FileWriter;
import java.lang.reflect.Field;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import lombok.extern.slf4j.Slf4j;

/**
 * Build a product listing from calling Shopify prouduct services
 */
@Slf4j
public class ProductListingBuilder {
    private static final Object[] CSV_HEADER = { "id", "title", "description", "image_link", "link", "availability", "condition", "price",
            "mpn" };

    private ShopifyClientService svc;

    public ProductListingBuilder(ShopifyClientService svc) {
        this.svc = svc;
    }

    /**
     * Produce a Gemini product feed if it has never been done before for this shopper
     * 
     * TODO: check whether the shop's feed has ever been produced
     */
    public void archetype() throws Exception {
        ShopifyProductData[] products = svc.get(ShopifyProductData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_ALL);

        if (products != null) {
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

            try (FileWriter csvWriter = new FileWriter("shopify-test.csv", false);
                    CSVPrinter csvFilePrinter = new CSVPrinter(csvWriter, csvFileFormat)) {

                // Create CSV file header
                csvFilePrinter.printRecord(CSV_HEADER);

                ShopifyProductVariantData[] variants;
                ShopifyProductImageData[] images;
                ProductRecordData geminiProduct = new ProductRecordData();

                for (ShopifyProductData p : products) {
                    variants = svc.get(ShopifyProductVariantData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_VARIANTS, p.getId());
                    images = svc.get(ShopifyProductImageData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_IMAGES, p.getId());

                    if (isEmpty(variants) || isEmpty(variants)) {
                        log.warn("shopify product {} is missing variant and/or images", p.getId());
                        continue;
                    }

                    p.setVariants(variants);
                    p.setImages(images);

                    geminiProduct.setId(p.getId());
                    geminiProduct.setTitle(p.getTitle());
                    geminiProduct.setDescription(p.getDescription());
                    geminiProduct.setImage_link(images[0].getSrc());
                    geminiProduct.setLink(svc.getShop());
                    geminiProduct.setAvailability(ProductConstant.Availability.IN_STOCK);
                    geminiProduct.setPrice(Float.toString(variants[0].getPrice()));
                    geminiProduct.setMpn(variants[0].getSku());
                    geminiProduct.setGtin(variants[0].getBarcode());

                    // Output one product
                    csvFilePrinter.printRecord(toRecordArray(geminiProduct));
                }
            } catch (Exception e) {
                log.error("Failed to generate a shopify product feed", e);
                throw e;
            }
        }
    }

    private static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    private static Object[] toRecordArray(ProductRecordData geminiProduct) throws Exception {
        String[] record = new String[CSV_HEADER.length];
        Field f;
        Object v;

        for (int i = 0; i < CSV_HEADER.length; i++) {
            if ("description".equals(CSV_HEADER[i])) {
                record[i] = "description placeholder";
            } else {
                f = geminiProduct.getClass().getDeclaredField((String) CSV_HEADER[i]);
                f.setAccessible(true);
                v = f.get(geminiProduct);
                record[i] = (v != null) ? v.toString() : null;
            }
        }
        return record;
    }
}
