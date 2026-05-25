package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.request.UploadExcelRequest;
import com.lufax.dashboard.model.response.UploadExcelResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link UploadController}.
 * Covers 2 endpoints: POST /excel, POST /data
 */
public class UploadControllerTest {

    @InjectMocks
    private UploadController uploadController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    // ========== POST /api/upload/excel ==========

    @Test
    public void testUploadExcel_Success() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test_data.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "fake excel content".getBytes()
        );

        // Act
        ResponseEntity<UploadExcelResponse> response = uploadController.uploadExcel(file, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        UploadExcelResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("test_data.xlsx", body.getFilename());
        assertEquals(Integer.valueOf(0), body.getRowsProcessed());
        assertEquals("File uploaded successfully", body.getMessage());
    }

    @Test
    public void testUploadExcel_WithScenarioParam() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "data".getBytes()
        );

        // Act
        ResponseEntity<UploadExcelResponse> response = uploadController.uploadExcel(file, "base");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("report.xlsx", response.getBody().getFilename());
    }

    @Test
    public void testUploadExcel_CsvFile() {
        // Arrange - accept CSV as well
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.csv", "text/csv",
                "col1,col2\nval1,val2".getBytes()
        );

        // Act
        ResponseEntity<UploadExcelResponse> response = uploadController.uploadExcel(file, "product_performance");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("data.csv", response.getBody().getFilename());
    }

    @Test
    public void testUploadExcel_DifferentFilenames() {
        // Test various filename patterns
        String[] filenames = {"financial_2026Q1.xlsx", "report_March.xls", "data-export.csv", "中文文件名.xlsx"};
        for (String filename : filenames) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/octet-stream",
                    "content".getBytes()
            );
            ResponseEntity<UploadExcelResponse> response = uploadController.uploadExcel(file, null);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(filename, response.getBody().getFilename());
        }
    }

    @Test
    public void testUploadExcel_EmptyFile() {
        // Arrange - empty file content should still be accepted (controller doesn't validate)
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.xlsx", "application/octet-stream",
                new byte[0]
        );

        // Act
        ResponseEntity<UploadExcelResponse> response = uploadController.uploadExcel(file, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("empty.xlsx", response.getBody().getFilename());
    }

    // ========== POST /api/upload/data ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testUploadData_SuccessWithRows() {
        // Arrange
        UploadExcelRequest request = new UploadExcelRequest();
        request.setScenario("base");
        List<Map<String, Object>> rows = Arrays.asList(
                createDataRow("A", 100.0),
                createDataRow("B", 200.0),
                createDataRow("C", 300.0)
        );
        request.setRows(rows);

        // Act
        ResponseEntity<Map<String, Object>> response = uploadController.uploadData(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("base", response.getBody().get("scenario"));
        assertEquals(3, response.getBody().get("rows_received"));
        assertEquals("Data received", response.getBody().get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUploadData_NullRows() {
        // Arrange
        UploadExcelRequest request = new UploadExcelRequest();
        request.setScenario("bear");
        request.setRows(null);

        // Act
        ResponseEntity<Map<String, Object>> response = uploadController.uploadData(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("bear", response.getBody().get("scenario"));
        assertEquals(0, response.getBody().get("rows_received"));  // null -> 0
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUploadData_EmptyRowsList() {
        // Arrange
        UploadExcelRequest request = new UploadExcelRequest();
        request.setScenario("channel_analysis");
        request.setRows(Arrays.asList());

        // Act
        ResponseEntity<Map<String, Object>> response = uploadController.uploadData(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().get("rows_received"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUploadData_LargePayload() {
        // Arrange - simulate a large data payload
        UploadExcelRequest request = new UploadExcelRequest();
        request.setScenario("risk_management");
        List<Map<String, Object>> manyRows = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            manyRows.add(createDataRow("row_" + i, (double) i));
        }
        request.setRows(manyRows);

        // Act
        ResponseEntity<Map<String, Object>> response = uploadController.uploadData(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1000, response.getBody().get("rows_received"));
        assertEquals("risk_management", response.getBody().get("scenario"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUploadData_SingleRow() {
        // Arrange
        UploadExcelRequest request = new UploadExcelRequest();
        request.setScenario("dashboard_overview");
        request.setRows(Arrays.asList(createDataRow("single", 42.0)));

        // Act
        ResponseEntity<Map<String, Object>> response = uploadController.uploadData(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().get("rows_received"));
        assertTrue((Boolean) response.getBody().get("success"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createDataRow(String name, Double value) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("amount", value);
        return row;
    }
}
