package com.souk.vendor.util;

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
 * Utility for parsing Excel files for vendor bulk uploads.
 *
 * Expected Excel format:
 * Column A: Vendor Name (required)
 * Column B: Email
 * Column C: Phone Number
 * Column D: Description
 * Column E: Address 1
 * Column F: Address 2
 * Column G: State
 * Column H: Pincode
 * Column I: Landmark
 * Column J: Contact Name
 * Column K: Latitude
 * Column L: Longitude
 * Column M: Supported Categories (JSON string array)
 * Column N: Schedule (JSON string)
 * Column O: Image URL
 */
public class VendorExcelParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class VendorRow {
        public String name;
        public String email;
        public String phoneNumber;
        public String description;
        public String address1;
        public String address2;
        public String state;
        public String pincode;
        public String landmark;
        public String contactName;
        public BigDecimal latitude;
        public BigDecimal longitude;
        public JsonNode supportedCategories;
        public JsonNode schedule;
        public String image;

        public boolean hasRequiredFields() {
            return name != null && !name.trim().isEmpty();
        }
    }

    /**
     * Parse Excel file and extract vendor rows
     */
    public static List<VendorRow> parseExcel(MultipartFile file) throws IOException {
        List<VendorRow> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                VendorRow vendorRow = parseRow(row);
                if (vendorRow.hasRequiredFields()) {
                    rows.add(vendorRow);
                }
            }
        }

        return rows;
    }

    private static VendorRow parseRow(Row row) {
        VendorRow vendorRow = new VendorRow();

        // Column A: Vendor Name (required)
        vendorRow.name = getCellValueAsString(row.getCell(0));

        // Column B: Email
        vendorRow.email = getCellValueAsString(row.getCell(1));

        // Column C: Phone Number
        vendorRow.phoneNumber = getCellValueAsString(row.getCell(2));

        // Column D: Description
        vendorRow.description = getCellValueAsString(row.getCell(3));

        // Column E: Address 1
        vendorRow.address1 = getCellValueAsString(row.getCell(4));

        // Column F: Address 2
        vendorRow.address2 = getCellValueAsString(row.getCell(5));

        // Column G: State
        vendorRow.state = getCellValueAsString(row.getCell(6));

        // Column H: Pincode
        vendorRow.pincode = getCellValueAsString(row.getCell(7));

        // Column I: Landmark
        vendorRow.landmark = getCellValueAsString(row.getCell(8));

        // Column J: Contact Name
        vendorRow.contactName = getCellValueAsString(row.getCell(9));

        // Column K: Latitude
        String latStr = getCellValueAsString(row.getCell(10));
        if (latStr != null && !latStr.isEmpty()) {
            try {
                vendorRow.latitude = new BigDecimal(latStr);
            } catch (NumberFormatException ignored) {}
        }

        // Column L: Longitude
        String lonStr = getCellValueAsString(row.getCell(11));
        if (lonStr != null && !lonStr.isEmpty()) {
            try {
                vendorRow.longitude = new BigDecimal(lonStr);
            } catch (NumberFormatException ignored) {}
        }

        // Column M: Supported Categories (JSON)
        String categoriesStr = getCellValueAsString(row.getCell(12));
        if (categoriesStr != null && !categoriesStr.isEmpty()) {
            try {
                vendorRow.supportedCategories = MAPPER.readTree(categoriesStr);
            } catch (Exception ignored) {}
        }

        // Column N: Schedule (JSON)
        String scheduleStr = getCellValueAsString(row.getCell(13));
        if (scheduleStr != null && !scheduleStr.isEmpty()) {
            try {
                vendorRow.schedule = MAPPER.readTree(scheduleStr);
            } catch (Exception ignored) {}
        }

        // Column O: Image URL
        vendorRow.image = getCellValueAsString(row.getCell(14));

        return vendorRow;
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
