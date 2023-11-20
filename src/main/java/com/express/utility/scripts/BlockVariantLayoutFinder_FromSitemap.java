package com.express.utility.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class BlockVariantLayoutFinder_FromSitemap {
    public static final String OUTPUT_FILE = "ProdUrls-Blocks-Input-All-Locales-Output-ViaSitemap.xlsx";
    public static final String SUCCESS_MESSAGE = OUTPUT_FILE + " written successfully";
    public static final String EXCEPTION_MESSAGE = "Exception occurred ";
    public static final String HTTP_GET_METHOD = "GET";
    public static final int INDEX_ZERO = 0;
     private static final String[] blockArr = {"animation","download-screens","layouts","quick-action-card","app-banner","embed",
            "legal","quick-action-cards","app-store-blade","faq","library-config","quick-action-hub","app-store-highlight","feature-grid-desktop",
            "link-list","quotes","banner","feature-list","linked-image","ratings","blog-posts","filter-pages","list","schemas","branch-io",
            "firefly-card","long-text","search-marquee","browse-by-category","floating-button","make-a-project","seo-nav","browse-by-collaboration",
            "floating-panel","marquee","shared","bubble-ui-button","fragment","modal","show-section-only","cards","full-width",
            "multifunction-button","split-action","carousel-card-mobile","fullscreen-marquee","page-list","steps","category-list",
            "fullscreen-marquee-desktop","plans-comparison","sticky-footer","chat","gen-ai-cards","playlist","sticky-promo-bar","checker-board",
            "hero-3d","premium-plan","submit-email","choose-your-path","hero-animation","pricing","table-of-contents","collapsible-card",
            "hero-animation-beta","pricing-columns","tags","color-how-to-carousel","hero-color","pricing-hub","template-list","columns",
            "hero-image","pricing-modal","template-list-ace","commerce-cta","how-to-steps","pricing-plan","template-x","contact",
            "how-to-steps-carousel","pricing-summary","toc","content-toggle","icon-list","promotion","toggle-bar","cta-carousel",
            "image-list","puf","tutorials","download-cards","inline-banner","quick-action","video-metadata"};
    public static final String MOBILE_WITH_SPACE = " Mobile";
    public static final String DESKTOP = "Desktop";
    public static final String DESKTOP_SPACE_MOBILE = DESKTOP + MOBILE_WITH_SPACE;
    public static final String SMALL_CASE_DESKTOP = "desktop";
    public static final String SMALL_CASE_MOBILE = "mobile";
    public static final String MOBILE_WITHOUT_SPACE = "Mobile";
    public static final String DOMAIN = "https://www.adobe.com";
    public static final String SITEMAP_XML = "https://www.adobe.com/express/sitemap-index.xml";

    public static void main(String args[]) {
        try {
            Map<String, String> pageLastModifiedMap = getPageLastModifiedMap();
            Map<String, BlockVariantLayoutPojo> urlBlockCountMap = processInputExcel(pageLastModifiedMap);
            createOutputExcel(urlBlockCountMap);
        }
        catch (Exception e) {
            System.out.println(EXCEPTION_MESSAGE + e);
        }
    }

    private static Map<String, String> getPageLastModifiedMap() {
        Map<String, String> pageLastModifiedMap = new HashMap<>();
        Map<String, String> sitemapUrls = buildUrlListFromSiteMap(SITEMAP_XML, "sitemap");
        for (Map.Entry<String, String> entry : sitemapUrls.entrySet()) {
            pageLastModifiedMap.putAll(buildUrlListFromSiteMap(entry.getKey(), "url"));
        }
        return pageLastModifiedMap;
    }

    private static Map<String, String> buildUrlListFromSiteMap(String pageUrl, String elementName) {
        String pageResponse = StringUtils.EMPTY;
        Map<String, String> pageLastModifiedMap = new HashMap<>();
        try {
            URL url = new URL(pageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            HttpURLConnection.setFollowRedirects(false);
            connection.setRequestMethod(HTTP_GET_METHOD);
            connection.connect();
            int urlResponseCode = connection.getResponseCode();
            if (urlResponseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String strCurrentLine;
                while ((strCurrentLine = br.readLine()) != null) {
                    sb.append(strCurrentLine);
                }
                pageResponse = sb.toString();
            }
            pageLastModifiedMap = processSitemapResponse(pageResponse, elementName);
        } catch (IOException e) {
            System.out.println("unable to parse XML " + e);
        }
        return pageLastModifiedMap;
    }

    private static Map<String, String> processSitemapResponse(String pageResponse, String elementName) {
        Map<String, String> pageLastModifiedMap = new HashMap<>();
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            File xmlFile = new File("xmlFile");
            FileUtils.writeStringToFile(xmlFile, pageResponse, StandardCharsets.UTF_8);
            org.w3c.dom.Document xmlContent = builder.parse(xmlFile);
            NodeList sitemapElements = xmlContent.getElementsByTagName(elementName);
            for (int i = 0; i < sitemapElements.getLength(); i++) {
                Node sitemapElement = sitemapElements.item(i);
                NodeList nestedSitemapElements = sitemapElement.getChildNodes();
                String url = StringUtils.EMPTY;
                String date = StringUtils.EMPTY;
                for (int j = 0; j < nestedSitemapElements.getLength(); j++) {
                    Node nestedSitemapElement = nestedSitemapElements.item(j);
                    if (StringUtils.equalsIgnoreCase("loc", nestedSitemapElement.getNodeName())) {
                        url = nestedSitemapElement.getTextContent();
                    }
                    if (StringUtils.equalsIgnoreCase("lastmod", nestedSitemapElement.getNodeName())) {
                        date = nestedSitemapElement.getTextContent();
                    }
                }
                pageLastModifiedMap.put(url, date);
            }
            FileUtils.delete(xmlFile);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            System.out.println("Unable to parse XML" + e);
        }
        return pageLastModifiedMap;
    }

    private static String getPageResponse(String pageUrl) {
        String pageResponse = StringUtils.EMPTY;
        try {
            URL url = new URL(pageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            HttpURLConnection.setFollowRedirects(false);
            connection.setRequestMethod(HTTP_GET_METHOD);
            connection.connect();
            int urlResponseCode = connection.getResponseCode();
            if (urlResponseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String strCurrentLine;
                while ((strCurrentLine = br.readLine()) != null) {
                    sb.append(strCurrentLine);
                }
                pageResponse = sb.toString();
            }
        } catch (IOException e) {

        }
        return pageResponse;
    }

    private static Map<String, BlockVariantLayoutPojo> processInputExcel(Map<String, String> pageLastModifiedMap) throws IOException {
        List<String> blockList = Arrays.asList(blockArr);
        Map<String, BlockVariantLayoutPojo> blockVariantLayoutMap = new HashMap<>();
        int stepSize = 25;
        int count = 0;
        for (Map.Entry<String, String> entry : pageLastModifiedMap.entrySet()) {
            String urlValue = entry.getKey();
            try {
                if (StringUtils.isNotBlank(urlValue)) {
                    count++;
                    URL url = new URL(urlValue);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    HttpURLConnection.setFollowRedirects(false);
                    connection.setRequestMethod(HTTP_GET_METHOD);
                    connection.connect();
                    int urlResponseCode = connection.getResponseCode();
                    if (urlResponseCode == HttpURLConnection.HTTP_SEE_OTHER
                            || urlResponseCode == HttpURLConnection.HTTP_MOVED_PERM
                            || urlResponseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                        continue;
                    } else {
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String strCurrentLine;
                        while ((strCurrentLine = br.readLine()) != null) {
                            sb.append(strCurrentLine);
                        }
                        String htmlContent = sb.toString();
                        Document document = Jsoup.parse(htmlContent);
                        Elements elementsWithClassAttribute = document.getElementsByAttributeStarting("class");
                        for (Element element : elementsWithClassAttribute) {
                            String blockName = StringUtils.EMPTY;
                            String variantName = StringUtils.EMPTY;
                            String layoutName = StringUtils.EMPTY;
                            String key = StringUtils.EMPTY;
                            String pageUrl = urlValue;
                            String language = StringUtils.substringBetween(pageUrl, "https://www.adobe.com/", "/");
                            if (StringUtils.equalsIgnoreCase(language, "express")) {
                                language = "en";
                            }
                            int occurrence = 0;
                            Set<String> classNames = element.classNames();
                            List<String> classNameList = new ArrayList<>(classNames);
                            for (String className : classNames) {
                                boolean hasWhiteListedClass = ArrayUtils.contains(blockArr, className);
                                if (hasWhiteListedClass) {
                                    occurrence = 1;
                                    blockName = className;
                                    classNameList = classNameList.stream().filter(name -> !blockList.contains(name)).collect(Collectors.toList());
                                    variantName = String.join(StringUtils.SPACE, classNameList);
                                    layoutName = getLayoutName(element, DESKTOP);
                                    layoutName = layoutName + getLayoutName(element, MOBILE_WITHOUT_SPACE);

                                    if (StringUtils.isEmpty(layoutName)) {
                                        Elements sectionMetadataDiv = element.parent().getElementsByClass("section-metadata");
                                        if (sectionMetadataDiv.isEmpty()) {
                                            layoutName = DESKTOP_SPACE_MOBILE;
                                        } else {
                                            for (Element sectionMetadata : sectionMetadataDiv) {
                                                Elements desktopDiv = sectionMetadata.getElementsMatchingOwnText(SMALL_CASE_DESKTOP);
                                                if (desktopDiv.size() > 0) {
                                                    layoutName = layoutName + DESKTOP;
                                                } else {
                                                    desktopDiv = sectionMetadata.getElementsMatchingOwnText(DESKTOP);
                                                    if (desktopDiv.size() > 0) {
                                                        layoutName = layoutName + DESKTOP;
                                                    }
                                                }
                                                Elements mobileDiv = sectionMetadata.getElementsMatchingOwnText(SMALL_CASE_MOBILE);
                                                if (mobileDiv.size() > 0) {
                                                    layoutName = layoutName + MOBILE_WITH_SPACE;
                                                } else {
                                                    mobileDiv = sectionMetadata.getElementsMatchingOwnText(MOBILE_WITHOUT_SPACE);
                                                    if (mobileDiv.size() > 0) {
                                                        layoutName = layoutName + MOBILE_WITH_SPACE;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    layoutName = layoutName.trim();
                                    key = blockName + variantName + layoutName + pageUrl;
                                    BlockVariantLayoutPojo blockVariantLayout = blockVariantLayoutMap.get(key);
                                    if (Objects.isNull(blockVariantLayout)) {
                                        String lastModifiedDate = pageLastModifiedMap.get(urlValue);
                                        BlockVariantLayoutPojo blockVariantLayoutPojo = new BlockVariantLayoutPojo(blockName, variantName, layoutName, pageUrl, language, occurrence, lastModifiedDate);
                                        blockVariantLayoutMap.put(key, blockVariantLayoutPojo);
                                    } else {
                                        int updatedOccurrence = blockVariantLayout.getOccurrence() + 1;
                                        blockVariantLayout.setOccurrence(updatedOccurrence);
                                        blockVariantLayoutMap.put(key, blockVariantLayout);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    connection.disconnect();
                    if (count % stepSize == 0) {
                        System.out.println("Processed: " + count + " URLs");
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception:" + urlValue);
            }
        }
        return blockVariantLayoutMap;
    }

    private static String getLayoutName(Element element, String searchLayout) {
        Elements childElements = element.children();
        if (childElements.isEmpty()) {
            return StringUtils.EMPTY;
        } else {
            for (Element childElement : childElements) {
                String divContent = childElement.ownText();
                if (StringUtils.equalsIgnoreCase(divContent, searchLayout)) {
                    return searchLayout;
                } else if (childElement.children().size() > 0) {
                    return getLayoutName(childElement, searchLayout);
                }
            }
        }
        return StringUtils.EMPTY;
    }

    private static void createOutputExcel(Map<String, BlockVariantLayoutPojo> urlBlockCountMap) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Production");
            int rowNum = INDEX_ZERO;
            XSSFRow headerRow = sheet.createRow(rowNum++);
            XSSFCell headerCell0 = headerRow.createCell(0);
            headerCell0.setCellValue("Block Name");
            XSSFCell headerCell1 = headerRow.createCell(1);
            headerCell1.setCellValue("Variant Name");
            XSSFCell headerCell2 = headerRow.createCell(2);
            headerCell2.setCellValue("Layout Name");
            XSSFCell headerCell3 = headerRow.createCell(3);
            headerCell3.setCellValue("Language");
            XSSFCell headerCell4 = headerRow.createCell(4);
            headerCell4.setCellValue("Occurrence");
            XSSFCell headerCell5 = headerRow.createCell(5);
            headerCell5.setCellValue("Last Modified");
            XSSFCell headerCell6 = headerRow.createCell(6);
            headerCell6.setCellValue("Page Url");
            for (Map.Entry<String, BlockVariantLayoutPojo> innerEntry : urlBlockCountMap.entrySet()) {
                XSSFRow row = sheet.createRow(rowNum++);
                XSSFCell urlCell = row.createCell(INDEX_ZERO);
                urlCell.setCellValue(innerEntry.getKey());
                try {
                    XSSFCell blockCell = row.createCell(0);
                    blockCell.setCellValue(innerEntry.getValue().getBlockName());
                    XSSFCell variantNameCell = row.createCell(1);
                    variantNameCell.setCellValue(innerEntry.getValue().getVariantName());
                    XSSFCell layoutNameCell = row.createCell(2);
                    layoutNameCell.setCellValue(innerEntry.getValue().getLayoutName());
                    XSSFCell pageUrlCell = row.createCell(3);
                    pageUrlCell.setCellValue(innerEntry.getValue().getLanguage());
                    XSSFCell occurrenceCell = row.createCell(4);
                    occurrenceCell.setCellValue(innerEntry.getValue().getOccurrence());
                    XSSFCell lastModifiedCell = row.createCell(5);
                    lastModifiedCell.setCellValue(innerEntry.getValue().getLastModified());
                    XSSFCell languageCell = row.createCell(6);
                    languageCell.setCellValue(innerEntry.getValue().getPageUrl());
                } catch (Exception e) {
                    System.out.println(EXCEPTION_MESSAGE + e);
                }
            }
            try {
                FileOutputStream out = new FileOutputStream(new File(OUTPUT_FILE));
                workbook.write(out);
                out.close();
                System.out.println(SUCCESS_MESSAGE);
            }
            catch (Exception e) {
                System.out.println(EXCEPTION_MESSAGE + e);
            }
        } catch (Exception e) {
            System.out.println(EXCEPTION_MESSAGE + e);
        }
    }
}
