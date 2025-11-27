package com.souk.product.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for parsing Excel files for product bulk updates.
 *
 * Expected Excel format:
 * Column A: SKU (required)
 * Column B: VendorID (required for new products, optional for updates)
 * Column C: Name
 * Column D: Price
 * Column E: Description
 * Column F: Available (true/false)
 * Column G: CategoryDetails (JSON string)
 * Column H: Schedule (JSON string)
 * Column I: UseVendorSchedule (true/false)
 * Column J: MediaUrls (pipe-separated URLs, e.g., "url1|url2|url3")
 */
public class ExcelParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class ProductRow {
        public String sku;
        public Long vendorId;
        public String name;
        public BigDecimal price;
        public String description;
        public Boolean available;
        public JsonNode categoryDetails;
        public JsonNode schedule;
        public Boolean useVendorSchedule;
        public List<String> mediaUrls;

        public boolean hasAnySku() {
            return sku != null && !sku.trim().isEmpty();
        }
    }

    /**
     * Parse Excel file and extract product rows
     */
    public static List<ProductRow> parseExcel(MultipartFile file) throws IOException {
        List<ProductRow> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ProductRow productRow = parseRow(row);
                if (productRow.hasAnySku()) {
                    rows.add(productRow);
                }
            }
        }

        return rows;
    }

    private static ProductRow parseRow(Row row) {
        ProductRow productRow = new ProductRow();

        // Column A: SKU (required)
        productRow.sku = getCellValueAsString(row.getCell(0));

        // Column B: VendorID (required for new products)
        String vendorIdStr = getCellValueAsString(row.getCell(1));
        if (vendorIdStr != null && !vendorIdStr.isEmpty()) {
            try {
                productRow.vendorId = Long.parseLong(vendorIdStr);
            } catch (NumberFormatException ignored) {}
        }

        // Column C: Name
        productRow.name = getCellValueAsString(row.getCell(2));

        // Column D: Price
        String priceStr = getCellValueAsString(row.getCell(3));
        if (priceStr != null && !priceStr.isEmpty()) {
            try {
                productRow.price = new BigDecimal(priceStr);
            } catch (NumberFormatException ignored) {}
        }

        // Column E: Description
        productRow.description = getCellValueAsString(row.getCell(4));

        // Column F: Available
        String availableStr = getCellValueAsString(row.getCell(5));
        if (availableStr != null && !availableStr.isEmpty()) {
            productRow.available = Boolean.parseBoolean(availableStr);
        }

        // Column G: CategoryDetails (JSON)
        String categoryDetailsStr = getCellValueAsString(row.getCell(6));
        if (categoryDetailsStr != null && !categoryDetailsStr.isEmpty()) {
            try {
                productRow.categoryDetails = MAPPER.readTree(categoryDetailsStr);
            } catch (Exception ignored) {}
        }

        // Column H: Schedule (JSON)
        String scheduleStr = getCellValueAsString(row.getCell(7));
        if (scheduleStr != null && !scheduleStr.isEmpty()) {
            try {
                productRow.schedule = MAPPER.readTree(scheduleStr);
            } catch (Exception ignored) {}
        }

        // Column I: UseVendorSchedule
        String useVendorScheduleStr = getCellValueAsString(row.getCell(8));
        if (useVendorScheduleStr != null && !useVendorScheduleStr.isEmpty()) {
            productRow.useVendorSchedule = Boolean.parseBoolean(useVendorScheduleStr);
        }

        // Column J: MediaUrls (pipe-separated)
        String mediaUrlsStr = getCellValueAsString(row.getCell(9));
        if (mediaUrlsStr != null && !mediaUrlsStr.isEmpty()) {
            productRow.mediaUrls = new ArrayList<>();
            String[] urls = mediaUrlsStr.split("\\|");
            for (String url : urls) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    productRow.mediaUrls.add(trimmed);
                }
            }
        }

        return productRow;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // If it's a whole number, don't include decimal
                    if (numValue == Math.floor(numValue)) {
                        yield String.valueOf((long) numValue);
                    }
                    yield String.valueOf(numValue);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception ex) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }
}
