package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.ClosableFTPClient;
import com.oath.gemini.merchant.ews.EWSConstant.PrdAvailabilityEnum;
import com.oath.gemini.merchant.ews.json.ProductRecordData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductImageData;
import com.oath.gemini.merchant.shopify.json.ShopifyProductVariantData;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.ftp.FTPFile;
import lombok.extern.slf4j.Slf4j;

/**
 * Build a product listing from calling Shopify prouduct services
 * 
 * @author tong on 10/1/2017
 */
@Slf4j
public class ShopifyProductSetBuilder {
    private static final Object[] CSV_HEADER = { "id", "title", "description", "image_link", "link", "availability", "condition", "price",
            "gtin", "mpn", "brand", "product_type" };

    private ShopifyClientService svc;
    private String localFile;
    private String remoteFile;
    private String productsRootUrl;
    private static final ExecutorService feedExecutor = Executors.newFixedThreadPool(1);
    private static final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public ShopifyProductSetBuilder(ShopifyClientService svc) {
        this.svc = svc;

        String baseName = svc.getShopName();
        localFile = baseName + ".csv";
        remoteFile = "/shopify/" + baseName + ".csv";

        // Build the root URL of all products
        productsRootUrl = svc.getShop().toLowerCase();

        if (!productsRootUrl.matches("^(http|https)://.*")) {
            productsRootUrl = "https://" + productsRootUrl;
        }

        int pathStart = productsRootUrl.indexOf('/', 8);
        if (pathStart > 0) {
            productsRootUrl = productsRootUrl.substring(0, pathStart);
        }
        productsRootUrl += "/products/";
    }

    /**
     * Produce a Gemini product feed if it has never been done before for this shopper. Since the feed could be huge, so run
     * it in background such that it will not interfere the onboarding of the Shopify app
     */
    public String uploadFeedIfRequiredAsync() throws Exception {
        try (ClosableFTPClient ftpClient = new ClosableFTPClient()) {
            if (!ftpClient.exits(remoteFile)) {
                feedExecutor.submit(new Callable<Integer>() {
                    public Integer call() {
                        try {
                            return uploadFeedIfRequired(ShopifyEndpointEnum.SHOPIFY_PROD_ALL);
                        } catch (Exception e) {
                            return 0;
                        }
                    }
                });
            }
        }

        return remoteFile;
    }

    /**
     * Update newly added or updated products
     * 
     * @return the count of the products fetched from Shopify and streamed to FTP
     */
    public int uploadFeedDelta(int freshnessInMinutes) throws Exception {
        String lastUpdated = null;

        try (ClosableFTPClient ftpClient = new ClosableFTPClient()) {
            FTPFile ftpFile = ftpClient.find(remoteFile);

            if (ftpFile != null) {
                Date lastMod = ftpFile.getTimestamp().getTime();
                if (freshnessInMinutes <= 0 || ftpClient.isFresh(lastMod.getTime(), freshnessInMinutes * 60000L)) {
                    lastUpdated = iso8601DateFormat.format(lastMod);
                } else {
                    log.debug("The feed='{}' is still fresh under '{}' minutes", remoteFile, freshnessInMinutes);
                    return 0;
                }
            }
        }

        if (lastUpdated == null) {
            return uploadFeedIfRequired(ShopifyEndpointEnum.SHOPIFY_PROD_ALL);
        } else {
            return uploadFeedIfRequired(ShopifyEndpointEnum.SHOPIFY_PROD_SINCE, lastUpdated);
        }
    }

    /**
     * Download products from Shopify and then upload the result to FTP server for Gemini
     * 
     * @return the count of the products fetched from Shopify and streamed to FTP
     */
    private int uploadFeedIfRequired(ShopifyEndpointEnum endpoint, Object... macros) throws Exception {
        ShopifyProductData[] products = svc.get(ShopifyProductData[].class, endpoint, macros);

        if (ArrayUtils.isEmpty(products)) {
            log.warn("No product found for the shop='{}'", svc.getShop());
        } else {
            try (ClosableFTPClient ftpClient = new ClosableFTPClient()) {
                // Copy a local file to FTP server
                toLocalCSVFile(products);
                ftpClient.copyTo(localFile, remoteFile);
                return products.length;
            }
        }
        return 0;
    }

    /**
     * Convert the Shopify products to the CSV form, and save the result to a given file
     */
    private void toLocalCSVFile(ShopifyProductData[] products) throws Exception {
        List<ProductRecordData> geminiProducts = new ArrayList<>();
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

        try (FileWriter csvWriter = new FileWriter(localFile, false);
                CSVPrinter csvFilePrinter = new CSVPrinter(csvWriter, csvFileFormat)) {

            // Create CSV file header
            csvFilePrinter.printRecord(CSV_HEADER);

            ShopifyProductVariantData[] variants;
            ShopifyProductImageData[] images;
            ProductRecordData geminiProduct = new ProductRecordData();

            for (ShopifyProductData p : products) {
                variants = svc.get(ShopifyProductVariantData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_VARIANTS, p.getId());
                images = svc.get(ShopifyProductImageData[].class, ShopifyEndpointEnum.SHOPIFY_PROD_IMAGES, p.getId());

                if (ArrayUtils.isEmpty(variants)) {
                    log.warn("shopify product {} is missing of variant objects", p.getId());
                    continue;
                }
                if (ArrayUtils.isEmpty(images)) {
                    log.warn("shopify product {} is missing of image objects", p.getId());
                    continue;
                }

                p.setVariants(variants);
                p.setImages(images);

                geminiProduct.setId(p.getId());
                geminiProduct.setTitle(p.getTitle());
                geminiProduct.setDescription(p.getDescription());
                geminiProduct.setImage_link(images[0].getSrc());
                geminiProduct.setLink(productsRootUrl + p.getHandle());
                geminiProduct.setAvailability(PrdAvailabilityEnum.IN_STOCK);
                geminiProduct.setPrice(Float.toString(variants[0].getPrice()));
                geminiProduct.setMpn(variants[0].getSku());
                geminiProduct.setGtin(variants[0].getBarcode());
                geminiProduct.setBrand(p.getBrand());
                geminiProduct.setProduct_type(p.getProduct_type());

                // Output one product
                csvFilePrinter.printRecord(toRecordArray(geminiProduct));
                geminiProducts.add(geminiProduct);
            }
        } catch (Exception e) {
            log.error("Failed to generate a shopify product feed", e);
            throw e;
        }
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
