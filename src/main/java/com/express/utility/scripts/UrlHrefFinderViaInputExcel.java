package com.express.utility.scripts;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class UrlHrefFinderViaInputExcel {

    public static final String INPUT_FILE = "ProdUrlsInput1000.xlsx";
    public static final String OUTPUT_FILE = "ProdUrlsOutput1000.xlsx";
    public static final String SUCCESS_MESSAGE = OUTPUT_FILE + " written successfully";
    public static final String EXCEPTION_MESSAGE = "Exception occurred ";
    public static final String FILE_NOT_FOUND_MESSAGE = "file not found! ";
    public static final String HTTP_GET_METHOD = "GET";
    public static final String URL = "Url";
    public static final int INDEX_ZERO = 0;
    public static final String DOMAIN = "adobe.com";
    public static final String HREF = "href";

    public static void main(String args[]) {
        UrlHrefFinderViaInputExcel urlStatusFinder = new UrlHrefFinderViaInputExcel();

        try {
            File inputFile = urlStatusFinder.getFileFromResource(INPUT_FILE);
            Map<String, String> urlBlockCountMap = processInputExcel(inputFile);
            createOutputExcel(urlBlockCountMap);
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

    private File getFileFromResource(String fileName) throws URISyntaxException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException(FILE_NOT_FOUND_MESSAGE + fileName);
        } else {
            return new File(resource.toURI());
        }
    }

    private static Map<String, String> processInputExcel(File inputFile) throws IOException {
        FileInputStream file = new FileInputStream(inputFile);
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        Map<String, String> urlHrefMap = new HashMap<>();
        int stepSize = 25;
        while (sheetIterator.hasNext()) {
            XSSFSheet sheet = (XSSFSheet) sheetIterator.next();
            String sheetName = sheet.getSheetName();
            System.out.println("Processing: " + sheetName);
            int count = 0;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int rowNum = row.getRowNum();
                if (rowNum != INDEX_ZERO) {
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        String urlValue = cell.getStringCellValue();
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
                                    Elements elementsWithHrefAttribute = document.getElementsByAttributeStarting(HREF);
                                    String href = StringUtils.EMPTY;
                                    for (Element element : elementsWithHrefAttribute) {
                                        String hrefValue = element.attr(HREF);
                                        if (StringUtils.containsIgnoreCase(hrefValue, DOMAIN)) {
                                            href = href + "\n" + hrefValue;
                                        }
                                    }
                                    urlHrefMap.put(urlValue, href);
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
                }
            }
        }
        file.close();
        return urlHrefMap;
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

    private static void createOutputExcel(Map<String, String> urlHrefMap) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Production");
            int rowNum = INDEX_ZERO;
            XSSFRow headerRow = sheet.createRow(rowNum++);
            XSSFCell headerCell0 = headerRow.createCell(0);
            headerCell0.setCellValue("URL");
            XSSFCell headerCell1 = headerRow.createCell(1);
            headerCell1.setCellValue("HREF LINKS");
            for (Map.Entry<String, String> innerEntry : urlHrefMap.entrySet()) {
                XSSFRow row = sheet.createRow(rowNum++);
                try {
                    XSSFCell urlCell = row.createCell(0);
                    urlCell.setCellValue(innerEntry.getKey());
                    XSSFCell hrefCell = row.createCell(1);
                    hrefCell.setCellValue(innerEntry.getValue());
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
