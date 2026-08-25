package com.studyagent.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studyagent.model.ActivityRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;


public class ApiClient {

     private final HttpClient httpClient;
     private final ObjectMapper objectMapper;
     private final String baseUrl;
     private final String reportPath;

     public ApiClient(String baseUrl) {
         this.baseUrl = baseUrl;
         this.reportPath = "/api/activity/report";
         this.httpClient = HttpClient.newHttpClient();
         this.objectMapper = new ObjectMapper();
         this.objectMapper.registerModule(new JavaTimeModule());
         this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     }

    public boolean report(List<ActivityRecord> records) {
        try {
            String json = objectMapper.writeValueAsString(records);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + reportPath))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Upload response: " + response.statusCode());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("Upload failed: " + e.getMessage());
            return false;
        }
    }

    public String toJson(List<ActivityRecord> records) {
        try {
            return objectMapper.writeValueAsString(records);
        } catch (Exception e) {
            return "[]";
        }
    }

}
