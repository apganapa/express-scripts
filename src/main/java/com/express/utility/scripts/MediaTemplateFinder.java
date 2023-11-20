package com.express.utility.scripts;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaTemplateFinder {

    public static final String INPUT_FILE = "blog-jp-urls.xlsx";
    public static final String OUTPUT_FILE = "blog-jp-urls-output.xlsx";
    public static final String SUCCESS_MESSAGE = OUTPUT_FILE + " written successfully";
    public static final String EXCEPTION_MESSAGE = "Exception occurred ";
    public static final String FILE_NOT_FOUND_MESSAGE = "file not found! ";
    public static final String TEMPLATE_IDS = "Template Ids";
    public static final String HTTP_GET_METHOD = "GET";
    public static final String URL = "Url";
    public static final int INDEX_ZERO = 0;
    public static final int INDEX_ONE = 1;
    private static final String TEMPLATE_ID_PATTERN = "media_(.*?)\\.(png|jpeg|mp4)";

    public static void main(String args[]) {
        MediaTemplateFinder urlStatusFinder = new MediaTemplateFinder();
        try {
            File inputFile = urlStatusFinder.getFileFromResource(INPUT_FILE);
            Map<String, Map<String, String>> sheetUrlStatusMap = processInputExcel(inputFile);
            createOutputExcel(sheetUrlStatusMap);
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

    private static Map<String, Map<String, String>> processInputExcel(File inputFile) throws IOException {
        FileInputStream file = new FileInputStream(inputFile);
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        Map<String, Map<String, String>> sheetUrlStatusMap = new HashMap<>();
        int stepSize = 25;
        while (sheetIterator.hasNext()) {
            XSSFSheet sheet = (XSSFSheet) sheetIterator.next();
            String sheetName = sheet.getSheetName();
            System.out.println("Processing: " + sheetName);
            int count = 0;
            Iterator<Row> rowIterator = sheet.iterator();
            Map<String, String> urlStatusMap = new HashMap<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int rowNum = row.getRowNum();
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String urlValue = cell.getStringCellValue();
                    try {
                        if (StringUtils.isNotBlank(urlValue)) {
                            if (rowNum != INDEX_ZERO) {
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
                                    Pattern p = Pattern.compile(TEMPLATE_ID_PATTERN);
                                    Matcher m = p.matcher(htmlContent);
                                    Set<String> templateIdSet = new HashSet<>();
                                    while (m.find()) {
                                        templateIdSet.add(m.group(1));
                                    }
                                    if (!templateIdSet.isEmpty()) {
                                        urlStatusMap.put(urlValue, templateIdSet.toString());
                                    }
                                }
                                connection.disconnect();
                                if (count % stepSize == 0) {
                                    System.out.println("Processed: " + count + " URLs");
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Exception:" + urlValue);
                    }
                }
                sheetUrlStatusMap.put(sheetName, urlStatusMap);
            }
        }
        file.close();
        return sheetUrlStatusMap;
    }

    private static void createOutputExcel(Map<String, Map<String, String>> sheetUrlStatusMap) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (Map.Entry<String, Map<String, String>> outerEntry : sheetUrlStatusMap.entrySet()) {
                String sheetName = outerEntry.getKey();
                XSSFSheet sheet = workbook.createSheet(sheetName);
                int rowNum = INDEX_ZERO;
                XSSFRow headerRow = sheet.createRow(rowNum++);
                XSSFCell headerCell1 = headerRow.createCell(INDEX_ZERO);
                headerCell1.setCellValue(URL);
                XSSFCell headerCell2 = headerRow.createCell(INDEX_ONE);
                headerCell2.setCellValue(TEMPLATE_IDS);
                for (Map.Entry<String, String> innerEntry : outerEntry.getValue().entrySet()) {
                    try {
                        XSSFRow row = sheet.createRow(rowNum++);
                        XSSFCell urlCell = row.createCell(INDEX_ZERO);
                        XSSFCell statusCodeCell = row.createCell(INDEX_ONE);
                        urlCell.setCellValue(innerEntry.getKey());
                        statusCodeCell.setCellValue(innerEntry.getValue());
                    } catch (Exception e) {
                        System.out.println(EXCEPTION_MESSAGE + e);
                        System.out.println(EXCEPTION_MESSAGE +" for: " + innerEntry.getKey());
                    }
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
