package com.oath.gemini.merchant.shopify;

import com.oath.gemini.merchant.feed.ProductRecordData;
import java.io.ByteArrayInputStream;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Fetch and parse a raw feed in W3 ATOM schema. Unfortunately, the feed itself does not contain all information offered
 * through Shopify product services.
 */
public class ProductListing {
    /**
     * Example:
     * 
     * <pre>
     * <feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom"
     *     xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" xmlns:s="http://jadedpixel.com/-/spec/shopify">
     *  <id>https://dpa-bridge.myshopify.com/collections/all.atom</id>
     *  <link rel="alternate" type="text/html" href="https://dpa-bridge.myshopify.com/collections/all"/>
     *  <link rel="self" type="application/atom+xml" href="https://dpa-bridge.myshopify.com/collections/all.atom"/>
     *  <title>DPA bridge</title>
     *  <updated>2017-08-30T18:19:06-04:00</updated>
     *  <author>
     *    <name>DPA bridge</name>
     *  </author>
     *  <entry>
     *    ....
     *  </entry>
     *    ....
     * </pre>
     */
    public static void fetch(ShopifyClientService svc) throws Exception {
        String raw = svc.get(ShopifyEndpointEnum.PROD_FEED);

        try (ByteArrayInputStream reader = new ByteArrayInputStream(raw.getBytes())) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(reader);

            NamedNodeMap ns = document.getDocumentElement().getAttributes();
            String shopifyNS = "";
            String atomNS = "";

            if (ns != null) {
                for (int i = 0; i < ns.getLength(); i++) {
                    Attr a = (Attr) ns.item(i);
                    String v = a.getValue();

                    if (v.contains("shopify")) {
                        int index = a.getName().indexOf(':');
                        if (index > 0) {
                            shopifyNS = a.getName().substring(index + 1) + ":";
                        }
                    } else if (v.contains("Atom")) {
                        int index = a.getName().indexOf(':');
                        if (index > 0) {
                            atomNS = a.getName().substring(index + 1) + ":";
                        }
                    }

                    System.out.println(a.getName() + " = " + v);
                }
            }

            // Return immediately if the feed has no change
            Element lastUpdated = getFirstElement(document.getElementsByTagName(atomNS + "updated"));
            if (!hasUpdated(lastUpdated, null)) {
                return;
            }

            NodeList entries = document.getElementsByTagName(atomNS + "entry");
            for (int i = 0; i < entries.getLength(); i++) {
                processProductEntry((Element) entries.item(i), atomNS, shopifyNS);
            }
        }
    }

    /**
     * Example:
     * 
     * <pre>
     * <entry>
     *   <id>https://dpa-bridge.myshopify.com/products/10559414664</id>
     *   <published>2017-08-30T18:19:06-04:00</published>
     *   <updated>2017-08-30T18:19:06-04:00</updated>
     *   <link rel="alternate" type="text/html" href="https://dpa-bridge.myshopify.com/products/waterproof-iphone-cases"/>
     *   <title>Waterproof iPhone Cases</title>
     *   <s:type></s:type>
     *   <s:vendor>DPA bridge</s:vendor>
     *   <summary type="html">
     *     ....
     *   </summary>
     *   <s:variant>
     *     ....
     *   </s:variant>
     *     ....
     *   <s:variant>
     *     ....
     *   </s:variant>
     *     ....
     * </entry>
     * </pre>
     */
    private static void processProductEntry(Element product, String atomNS, String shopifyNS) {
        NodeList variants = product.getElementsByTagName(shopifyNS + "variant");

        if (variants != null) {
            for (int i = 0; i < variants.getLength(); i++) {
                processVariantEntry((Element) variants.item(i), atomNS, shopifyNS);
            }
        }
    }

    /**
     * Example:
     * 
     * <pre>
     *   <s:variant>
     *     <id>https://dpa-bridge.myshopify.com/products/10559414664</id>
     *     <title>Black and White / For Iphone 7</title>
     *     <s:price currency="USD">5.58</s:price>
     *     <s:sku>PT3882T8</s:sku>
     *     <s:grams>0</s:grams>
     *   </s:variant>
     * </pre>
     */
    private static ProductRecordData processVariantEntry(Element variant, String atomNS, String shopifyNS) {
        NodeList children = variant.getChildNodes();
        ProductRecordData productRecordData = null;

        if (children != null) {
            productRecordData = new ProductRecordData();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                Node text = child.getFirstChild();

                switch (child.getLocalName()) {
                case "id":
                    break;
                case "title":
                    productRecordData.setTitle(text.getNodeValue());
                    break;
                case "price":
                    productRecordData.setPrice(text.getNodeValue());
                    break;
                case "sku": // "stock keeping unit", which is not the same but close to "manufacturer part number"
                    productRecordData.setMpn(text.getNodeValue());
                    break;
                }
            }
        }
        return productRecordData;
    }

    private static Element getFirstElement(NodeList elements) {
        if (elements != null && elements.getLength() >= 1) {
            return (Element) elements.item(0);
        }
        return null;
    }

    private static boolean hasUpdated(Element lastUpdated, Date previousData) {
        return true;
    }
}
