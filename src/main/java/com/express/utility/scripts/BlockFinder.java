package com.express.utility.scripts;

import org.apache.commons.lang3.ArrayUtils;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BlockFinder {

    public static final String INPUT_FILE = "ProdUrls-EN-Templates-Blocks-Input.xlsx";
    public static final String OUTPUT_FILE = "ProdUrls-EN-Templates-Blocks-Output.xlsx";
    public static final String SUCCESS_MESSAGE = OUTPUT_FILE + " written successfully";
    public static final String EXCEPTION_MESSAGE = "Exception occurred ";
    public static final String FILE_NOT_FOUND_MESSAGE = "file not found! ";
    public static final String HTTP_GET_METHOD = "GET";
    public static final String URL = "Url";
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

    public static void main(String args[]) {
        BlockFinder urlStatusFinder = new BlockFinder();
        try {
            File inputFile = urlStatusFinder.getFileFromResource(INPUT_FILE);
            Map<String, Map<String, Integer>> urlBlockCountMap = processInputExcel(inputFile);
            createOutputExcel(urlBlockCountMap);
        }
        catch (Exception e) {
            System.out.println(EXCEPTION_MESSAGE + e);
        }
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

    private static void setInitialValue(String urlValue, Map<String, Map<String, Integer>> urlBlockCountMap) {
        Map<String, Integer> initialBlockCountMap = new HashMap<>();
        for (String blockName : blockArr) {
            initialBlockCountMap.put(blockName, 0);
        }
        urlBlockCountMap.put(urlValue, initialBlockCountMap);
    }

    private static Map<String, Map<String, Integer>> processInputExcel(File inputFile) throws IOException {
        FileInputStream file = new FileInputStream(inputFile);
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        Map<String, Map<String, Integer>> urlBlockCountMap = new HashMap<>();
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
                                setInitialValue(urlValue, urlBlockCountMap);
                                Map<String, Integer> blockCountMap = urlBlockCountMap.get(urlValue);
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
                                        Set<String> classNames = element.classNames();
                                        for (String className : classNames) {
                                            boolean hasWhiteListedClass = ArrayUtils.contains(blockArr, className);
                                            if (hasWhiteListedClass) {
                                                Integer counter = blockCountMap.get(className);
                                                counter = counter + 1;
                                                blockCountMap.put(className, counter);
                                            }
                                        }
                                    }
                                }
                                urlBlockCountMap.put(urlValue, blockCountMap);
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
        return urlBlockCountMap;
    }

    private static void createOutputExcel(Map<String, Map<String, Integer>> urlBlockCountMap) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Production");
            int rowNum = INDEX_ZERO;
            XSSFRow headerRow = sheet.createRow(rowNum++);
            XSSFCell headerCell1 = headerRow.createCell(INDEX_ZERO);
            headerCell1.setCellValue(URL);
            int headerCellIndex = 1;
            for (Map.Entry<String, Map<String, Integer>> innerEntry : urlBlockCountMap.entrySet()) {
                for (Map.Entry<String, Integer> countEntry : innerEntry.getValue().entrySet()) {
                    XSSFCell headerCell = headerRow.createCell(headerCellIndex++);
                    headerCell.setCellValue(countEntry.getKey());
                }
                break;
            }
            for (Map.Entry<String, Map<String, Integer>> innerEntry : urlBlockCountMap.entrySet()) {
                XSSFRow row = sheet.createRow(rowNum++);
                XSSFCell urlCell = row.createCell(INDEX_ZERO);
                urlCell.setCellValue(innerEntry.getKey());
                int cellIndex = 1;
                try {
                    for (Map.Entry<String, Integer> countEntry : innerEntry.getValue().entrySet()){
                        XSSFCell blockCell = row.createCell(cellIndex++);
                        blockCell.setCellValue(countEntry.getValue());
                    }
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
