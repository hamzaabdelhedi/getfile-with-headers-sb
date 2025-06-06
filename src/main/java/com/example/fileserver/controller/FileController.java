package com.example.fileserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@Tag(name = "File Operations", description = "API for file operations")
public class FileController {

    @GetMapping("/get_my_file")
    @Operation(
        summary = "Download a file from the server",
        description = "Fetch a file from the host filesystem and stream it back to the client with chunked transfer encoding"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file path"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> getMyFile(
            @Parameter(description = "Relative path to the file to retrieve from the server", example = "documents/example.txt")
            @RequestParam("file_path") String filePath,
            HttpServletResponse response) {
        
        try {
            // Security check: prevent directory traversal attacks
            if (filePath.contains("..") || filePath.startsWith("/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }
            
            // Create Path object
            Path path = Paths.get(filePath);
            File file = path.toFile();
            
            // Check if file exists
            if (!file.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            
            // Check if it's actually a file (not a directory)
            if (!file.isFile()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path is not a file");
            }
            
            // Determine content type
            String contentType = determineContentType(filePath);
            
            // Create InputStreamResource for streaming
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.TRANSFER_ENCODING, "chunked");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
                    
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error processing file: " + e.getMessage());
        }
    }
    
    @GetMapping("/")
    @Operation(
        summary = "API Information",
        description = "Get basic information about the File Server API"
    )
    public Map<String, Object> root() {
        return Map.of(
            "message", "File Server API is running",
            "endpoints", new String[]{"/get_my_file"}
        );
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Health Check",
        description = "Check if the API is running and healthy"
    )
    public Map<String, String> healthCheck() {
        return Map.of("status", "healthy");
    }
    
    private String determineContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        
        if (lowerPath.endsWith(".txt") || lowerPath.endsWith(".log")) {
            return "text/plain";
        } else if (lowerPath.endsWith(".json")) {
            return "application/json";
        } else if (lowerPath.endsWith(".html") || lowerPath.endsWith(".htm")) {
            return "text/html";
        } else if (lowerPath.endsWith(".css")) {
            return "text/css";
        } else if (lowerPath.endsWith(".js")) {
            return "application/javascript";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }
}