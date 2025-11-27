package com.souk.product.util;

import com.souk.common.domain.Vendor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Utility for generating Excel template for product bulk upload
 * with vendor dropdown functionality and sample data rows
 */
public class ExcelTemplateGenerator {

    /**
     * Generate Excel template with vendor dropdown in VendorID column
     */
    public static byte[] createProductBulkUploadTemplate(List<Vendor> vendors) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // Create main "Products" sheet
            Sheet productsSheet = workbook.createSheet("Products");

            // Create hidden "Vendors" reference sheet
            Sheet vendorsSheet = workbook.createSheet("Vendors");
            workbook.setSheetHidden(1, true); // Hide the vendors sheet

            // Populate vendor reference data
            populateVendorSheet(vendorsSheet, vendors);

            // Create named range for vendor IDs
            createVendorNamedRange(workbook, vendors.size());

            // Create header row in products sheet
            createHeaderRow(productsSheet);

            // Add sample data rows to show format
            addSampleRows(productsSheet, vendors);

            // Add data validation for VendorID column (Column B)
            addVendorDropdown(productsSheet, vendors.size());

            // Set explicit column widths (in units of 1/256th of a character width)
            // This avoids the AWT dependency required by autoSizeColumn
            productsSheet.setColumnWidth(0, 4000);  // SKU
            productsSheet.setColumnWidth(1, 3000);  // VendorID
            productsSheet.setColumnWidth(2, 8000);  // Name
            productsSheet.setColumnWidth(3, 3000);  // Price
            productsSheet.setColumnWidth(4, 10000); // Description
            productsSheet.setColumnWidth(5, 3000);  // Available
            productsSheet.setColumnWidth(6, 8000);  // CategoryDetails
            productsSheet.setColumnWidth(7, 8000);  // Schedule
            productsSheet.setColumnWidth(8, 5000);  // UseVendorSchedule
            productsSheet.setColumnWidth(9, 8000);  // MediaUrls

            // Convert workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Populate hidden vendor reference sheet with vendor IDs and names
     */
    private static void populateVendorSheet(Sheet vendorsSheet, List<Vendor> vendors) {
        // Header row
        Row headerRow = vendorsSheet.createRow(0);
        headerRow.createCell(0).setCellValue("VendorID");
        headerRow.createCell(1).setCellValue("VendorName");

        // Create numeric cell style for vendor IDs
        CellStyle numericStyle = vendorsSheet.getWorkbook().createCellStyle();
        numericStyle.setDataFormat(vendorsSheet.getWorkbook().createDataFormat().getFormat("0"));

        // Data rows
        for (int i = 0; i < vendors.size(); i++) {
            Vendor vendor = vendors.get(i);
            Row row = vendorsSheet.createRow(i + 1);

            // Set vendor ID as numeric value
            Cell idCell = row.createCell(0);
            idCell.setCellValue(vendor.getVendorId());
            idCell.setCellStyle(numericStyle);

            row.createCell(1).setCellValue(vendor.getName());
        }

        // Set explicit column widths to avoid AWT dependency
        vendorsSheet.setColumnWidth(0, 3000);  // VendorID
        vendorsSheet.setColumnWidth(1, 6000);  // VendorName
    }

    /**
     * Add sample data rows to demonstrate format
     */
    private static void addSampleRows(Sheet productsSheet, List<Vendor> vendors) {
        if (vendors.isEmpty()) {
            // Add sample rows with placeholder vendor ID
            addSampleRow(productsSheet, 1, "SAMPLE-001", "1", "Sample Product 1", "9.99");
            addSampleRow(productsSheet, 2, "SAMPLE-002", "1", "Sample Product 2", "19.99");
        } else {
            // Add sample rows with actual vendor IDs
            addSampleRow(productsSheet, 1, "SAMPLE-001", String.valueOf(vendors.get(0).getVendorId()),
                        "Sample Product 1", "9.99");
            if (vendors.size() > 1) {
                addSampleRow(productsSheet, 2, "SAMPLE-002", String.valueOf(vendors.get(1).getVendorId()),
                            "Sample Product 2", "19.99");
            } else {
                addSampleRow(productsSheet, 2, "SAMPLE-002", String.valueOf(vendors.get(0).getVendorId()),
                            "Sample Product 2", "19.99");
            }
        }
    }

    /**
     * Add a single sample data row
     */
    private static void addSampleRow(Sheet sheet, int rowNum, String sku, String vendorId,
                                    String name, String price) {
        Row row = sheet.createRow(rowNum);

        // Create cell style for sample data (gray text)
        CellStyle sampleStyle = sheet.getWorkbook().createCellStyle();
        Font sampleFont = sheet.getWorkbook().createFont();
        sampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        sampleFont.setItalic(true);
        sampleStyle.setFont(sampleFont);

        // Create numeric style for vendor ID
        CellStyle sampleNumericStyle = sheet.getWorkbook().createCellStyle();
        sampleNumericStyle.cloneStyleFrom(sampleStyle);
        sampleNumericStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("0"));

        Cell cell0 = row.createCell(0);
        cell0.setCellValue(sku);
        cell0.setCellStyle(sampleStyle);

        Cell cell1 = row.createCell(1);
        cell1.setCellValue(Double.parseDouble(vendorId));  // Set as numeric value
        cell1.setCellStyle(sampleNumericStyle);

        Cell cell2 = row.createCell(2);
        cell2.setCellValue(name);
        cell2.setCellStyle(sampleStyle);

        Cell cell3 = row.createCell(3);
        cell3.setCellValue(price);
        cell3.setCellStyle(sampleStyle);

        Cell cell4 = row.createCell(4);
        cell4.setCellValue("Sample description");
        cell4.setCellStyle(sampleStyle);

        Cell cell5 = row.createCell(5);
        cell5.setCellValue("true");
        cell5.setCellStyle(sampleStyle);

        Cell cell6 = row.createCell(6);
        cell6.setCellValue("{\"categoryId\":1,\"subcategoryId\":1}");
        cell6.setCellStyle(sampleStyle);

        Cell cell7 = row.createCell(7);
        cell7.setCellValue("");
        cell7.setCellStyle(sampleStyle);

        Cell cell8 = row.createCell(8);
        cell8.setCellValue("true");
        cell8.setCellStyle(sampleStyle);

        Cell cell9 = row.createCell(9);
        cell9.setCellValue("");
        cell9.setCellStyle(sampleStyle);
    }

    /**
     * Create header row in products sheet
     */
    private static void createHeaderRow(Sheet productsSheet) {
        Row headerRow = productsSheet.createRow(0);

        // Create header style
        CellStyle headerStyle = productsSheet.getWorkbook().createCellStyle();
        Font headerFont = productsSheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {
            "SKU",
            "VendorID",
            "Name",
            "Price",
            "Description",
            "Available",
            "CategoryDetails",
            "Schedule",
            "UseVendorSchedule",
            "MediaUrls"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Create a named range for vendor IDs to use in data validation
     */
    private static void createVendorNamedRange(XSSFWorkbook workbook, int vendorCount) {
        if (vendorCount == 0) return;

        // Create a named range called "VendorList" pointing to the vendor IDs in the hidden sheet
        Name namedRange = workbook.createName();
        namedRange.setNameName("VendorList");
        String reference = "Vendors!$A$2:$A$" + (vendorCount + 1);
        namedRange.setRefersToFormula(reference);
    }

    /**
     * Add data validation dropdown for VendorID column
     */
    private static void addVendorDropdown(Sheet productsSheet, int vendorCount) {
        if (vendorCount == 0) return;

        DataValidationHelper validationHelper = productsSheet.getDataValidationHelper();

        // Use the named range "VendorList" for the dropdown
        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint("VendorList");

        // Apply validation to VendorID column (Column B) for rows 2 to 1000 (including sample rows)
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 1, 1);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);

        // Set validation options - critical for Excel dropdown to appear
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Invalid Vendor", "Please select a valid Vendor ID from the dropdown");
        validation.setShowPromptBox(true);
        validation.createPromptBox("Select Vendor", "Choose a vendor ID from the dropdown");
        validation.setEmptyCellAllowed(true);
        validation.setSuppressDropDownArrow(false);  // Show dropdown arrow

        productsSheet.addValidationData(validation);
    }
}
