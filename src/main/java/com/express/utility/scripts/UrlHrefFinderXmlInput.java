package com.express.utility.scripts;

import org.apache.commons.io.FileUtils;
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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UrlHrefFinderXmlInput {

    public static final String OUTPUT_FILE = "ProdUrlsOutput-XMLInput.xlsx";
    public static final String SUCCESS_MESSAGE = OUTPUT_FILE + " written successfully";
    public static final String EXCEPTION_MESSAGE = "Exception occurred ";
    public static final String HTTP_GET_METHOD = "GET";
    public static final int INDEX_ZERO = 0;
    public static final String DOMAIN = "adobe.com";
    public static final String HREF = "href";
    public static final String SITEMAP_XML = "https://www.adobe.com/express/sitemap-index.xml";

    public static void main(String args[]) {
        try {
            Map<String, UrlHrefPojo> urlHrefMap = new HashMap<>();
            List<String> pageUrls = new ArrayList<>();
            List<String> sitemapUrls = buildUrlListFromSiteMap(SITEMAP_XML, "sitemap");
            for (String sitemapUrl : sitemapUrls) {
                pageUrls.addAll(buildUrlListFromSiteMap(sitemapUrl, "url"));
            }
            processInputExcel(pageUrls, urlHrefMap);
            createOutputExcel(urlHrefMap);
        }
        catch (Exception e) {
            System.out.println(EXCEPTION_MESSAGE + e);
        }
    }

    private static List<String> buildUrlListFromSiteMap(String pageUrl, String elementName) {
        String pageResponse = StringUtils.EMPTY;
        List<String> pageUrlList = new ArrayList<>();
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
            pageUrlList = processSitemapResponse(pageResponse, elementName);
        } catch (IOException e) {
            System.out.println("unable to parse XML " + e);
        }
        return pageUrlList;
    }

    private static List<String> processSitemapResponse(String pageResponse, String elementName) {
        List<String> pageUrlList = new ArrayList<>();
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
                for (int j = 0; j < nestedSitemapElements.getLength(); j++) {
                    Node nestedSitemapElement = nestedSitemapElements.item(j);
                    if (StringUtils.equalsIgnoreCase("loc", nestedSitemapElement.getNodeName())) {
                        pageUrlList.add(nestedSitemapElement.getTextContent());
                    }
                }
            }
            FileUtils.delete(xmlFile);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            System.out.println("Unable to parse XML" + e);
        }
        return pageUrlList;
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

    private static Map<String, UrlHrefPojo> processInputExcel(List<String> pageUrls, Map<String, UrlHrefPojo> urlHrefMap) throws IOException {
        int stepSize = 25;
        int count = 0;
        int pageUrlIgnoreCount = 0;
        Set<String> uniqueDomainSet = new HashSet<>();
        int listSize = pageUrls.size();

            for (String pageUrl : pageUrls) {
                try {
                    URL url = new URL(pageUrl);
                    count++;
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    HttpURLConnection.setFollowRedirects(false);
                    connection.setRequestMethod(HTTP_GET_METHOD);
                    connection.connect();
                    int urlResponseCode = connection.getResponseCode();
                    if (urlResponseCode == HttpURLConnection.HTTP_SEE_OTHER
                            || urlResponseCode == HttpURLConnection.HTTP_MOVED_PERM
                            || urlResponseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                        pageUrlIgnoreCount++;
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
                        Elements elementsWithHrefAttribute = document.getElementsByAttributeStarting(HREF);
                        String href = StringUtils.EMPTY;
                        String adobeDomain = StringUtils.EMPTY;
                        String nonAdobeDomain = StringUtils.EMPTY;
                        for (Element element : elementsWithHrefAttribute) {
                            String hrefValue = element.attr(HREF);
                            String uniqueDomain = StringUtils.EMPTY;
                            if (!StringUtils.contains(href, hrefValue)) {
                                try {
                                    URL hrefAsUrl = new URL(hrefValue);
                                    if (hrefAsUrl != null) {
                                        uniqueDomain = hrefAsUrl.getProtocol() + "://" + hrefAsUrl.getHost();
                                    }
                                    if (StringUtils.containsIgnoreCase(uniqueDomain, DOMAIN)) {
                                        href = href + "\n" + hrefValue;
                                        if (!StringUtils.contains(adobeDomain, uniqueDomain)) {
                                            adobeDomain = adobeDomain + "\n" + uniqueDomain;
                                        }
                                        uniqueDomainSet.add(uniqueDomain);
                                    } else {
                                        href = href + "\n" + hrefValue;
                                        if (!StringUtils.contains(nonAdobeDomain, uniqueDomain)) {
                                            nonAdobeDomain = nonAdobeDomain + "\n" + uniqueDomain;
                                        }
                                        uniqueDomainSet.add(uniqueDomain);
                                    }
                                } catch (MalformedURLException e) {
                                    //System.out.println("Url " + hrefValue + " cannot be processed");
                                }
                            }
                        }
                        UrlHrefPojo urlHrefPojo = new UrlHrefPojo(href, adobeDomain, nonAdobeDomain);
                        urlHrefMap.put(pageUrl, urlHrefPojo);
                    }
                    connection.disconnect();
                    if (count % stepSize == 0) {
                        System.out.println("Processed: " + count + " URLs out of " + listSize);
                    }
                } catch (IOException e) {
                    System.out.println(e);
                    pageUrlIgnoreCount++;
                }
            }
            for (String uniqueDomain : uniqueDomainSet) {
                System.out.println(uniqueDomain);
            }
        return urlHrefMap;
    }

    private static void createOutputExcel(Map<String, UrlHrefPojo> urlHrefMap) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Production");
            int rowNum = INDEX_ZERO;
            XSSFRow headerRow = sheet.createRow(rowNum++);
            XSSFCell headerCell0 = headerRow.createCell(0);
            headerCell0.setCellValue("URL");
            XSSFCell headerCell1 = headerRow.createCell(1);
            headerCell1.setCellValue("HREF LINKS");
            XSSFCell headerCell2 = headerRow.createCell(2);
            headerCell2.setCellValue("ADOBE DOMAINS");
            XSSFCell headerCell3 = headerRow.createCell(3);
            headerCell3.setCellValue("NON-ADOBE DOMAINS");
            for (Map.Entry<String, UrlHrefPojo> innerEntry : urlHrefMap.entrySet()) {
                XSSFRow row = sheet.createRow(rowNum++);
                try {
                    XSSFCell urlCell = row.createCell(0);
                    urlCell.setCellValue(innerEntry.getKey());
                    XSSFCell hrefCell = row.createCell(1);
                    hrefCell.setCellValue(innerEntry.getValue().getHref());
                    XSSFCell adobeDomainCell = row.createCell(2);
                    adobeDomainCell.setCellValue(innerEntry.getValue().getUniqueAdobeDomain());
                    XSSFCell nonAdobeDomainCell = row.createCell(3);
                    nonAdobeDomainCell.setCellValue(innerEntry.getValue().getUniqueNonAdobeDomain());
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
