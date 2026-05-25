package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.request.UploadExcelRequest;
import com.lufax.dashboard.model.response.UploadExcelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    @PostMapping("/excel")
    public ResponseEntity<UploadExcelResponse> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String scenario) {
        logger.info("Received excel upload request, filename: {}, scenario: {}",
                file.getOriginalFilename(), scenario);

        UploadExcelResponse response = new UploadExcelResponse();
        response.setSuccess(true);
        response.setFilename(file.getOriginalFilename());
        response.setRowsProcessed(0);
        response.setMessage("File uploaded successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> uploadData(@RequestBody UploadExcelRequest request) {
        logger.info("Received data upload request for scenario: {}", request.getScenario());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("scenario", request.getScenario());
        result.put("rows_received", request.getRows() != null ? request.getRows().size() : 0);
        result.put("message", "Data received");

        return ResponseEntity.ok(result);
    }
}