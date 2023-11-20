package com.express.utility.scripts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class UrlHrefFinderWithUniqueDomain {

    public static final String OUTPUT_FILE = "ProdUrlsOutput10.xlsx";
    public static final String SUCCESS_MESSAGE = OUTPUT_FILE + " written successfully";
    public static final String EXCEPTION_MESSAGE = "Exception occurred ";
    public static final String FILE_NOT_FOUND_MESSAGE = "file not found! ";
    public static final String HTTP_GET_METHOD = "GET";
    public static final String URL = "Url";
    public static final int INDEX_ZERO = 0;
    public static final String DOMAIN = "adobe.com";
    public static final String HREF = "href";

    public static final String EXPRESS_PAGE_QUERY_INDEX_URL = "https://www.adobe.com%s/express/query-index.json";
    public static final String TEMPLATE_PAGE_QUERY_INDEX_URL = "https://www.adobe.com%s/express/learn/blog/query-index.json";
    public static final String[] expressPageLocaleArray = {"", "/br", "/cn", "/de", "/dk", "/es", "/fi", "/fr", "/in", "/jp", "/kr", "/mx", "/nl", "/no", "/se", "/tw"};
    public static final String[] blogPageLocaleArray = {"", "/jp", "/de", "/fr", "/es", "/br", "/it"};

    public static final String ADOBE_DOMAIN = "https://www.adobe.com";

    public static void main(String args[]) {
        try {
            Map<String, UrlHrefPojo> urlHrefMap = new HashMap<>();
            processInputExcel(expressPageLocaleArray, EXPRESS_PAGE_QUERY_INDEX_URL, urlHrefMap);
            processInputExcel(blogPageLocaleArray, TEMPLATE_PAGE_QUERY_INDEX_URL, urlHrefMap);
            createOutputExcel(urlHrefMap);
        }
        catch (Exception e) {
            System.out.println(EXCEPTION_MESSAGE + e);
        }
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

    private static Map<String, UrlHrefPojo> processInputExcel(String[] pageLocaleArray, String pageQueryIndexUrl, Map<String, UrlHrefPojo> urlHrefMap) throws IOException {
        int stepSize = 25;
        int count = 0;
        Set<String> uniqueDomainSet = new HashSet<>();
        try {
            for (String locale : pageLocaleArray) {
                String pageUrl = String.format(pageQueryIndexUrl, locale);
                String pageResponse = getPageResponse(pageUrl);
                if (StringUtils.isNotBlank(pageResponse)) {
                    JsonObject jsonObject = new Gson().fromJson(pageResponse, JsonObject.class);
                    if (Objects.nonNull(jsonObject)) {
                        JsonElement dataElement = jsonObject.get("data");
                        if (Objects.nonNull(dataElement)) {
                            JsonArray pageDataArray = dataElement.getAsJsonArray();
                            int pageUrlCounter = 0;
                            int pageUrlIgnoreCount = 0;
                            for (JsonElement jsonElement : pageDataArray) {
                                pageUrlCounter++;
                                String pagePath = jsonElement.getAsJsonObject().get("path").getAsString();
                                String fullPagePath = ADOBE_DOMAIN + pagePath;
                                URL url = new URL(fullPagePath);
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
                                    urlHrefMap.put(fullPagePath, urlHrefPojo);
                                }
                                connection.disconnect();
                                if (count % stepSize == 0) {
                                    //System.out.println("Processed: " + count + " URLs");
                                }
                            }
                            System.out.println("Page " + pageUrl + " has " + pageUrlCounter + " pages" + "processed " + (pageUrlCounter - pageUrlIgnoreCount) + " and Ignored " + pageUrlIgnoreCount);
                        }
                    }
                }
            }
            for (String uniqueDomain : uniqueDomainSet) {
                System.out.println(uniqueDomain);
            }
        } catch (IOException e) {
            System.out.println(e);
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
