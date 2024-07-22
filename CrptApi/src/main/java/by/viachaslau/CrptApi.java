package by.viachaslau;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final RateLimiter rateLimiter;
    private final HttpClient httpClient;
    private final String apiUrl;

    public CrptApi(TimeUnit timeUnit, int requestLimit, String apiUrl, HttpClient httpClient) {
        double rate = (double) requestLimit / timeUnit.toSeconds(1);
        this.rateLimiter = RateLimiter.create(rate);
        this.httpClient = httpClient;
        this.apiUrl = apiUrl;
    }

    public CompletableFuture<Void> createDocument(Document document, String signature) {
        return CompletableFuture.runAsync(() -> {
            try {
                rateLimiter.acquire();
                HttpRequest request = buildRequest(document, signature);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                processResponse(response);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        });
    }

    private void processResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        if (statusCode == 200) {
            System.out.println("Document successfully created! Response: " + responseBody);
        } else {
            System.err.println("Failed to create the document. Status code: " + statusCode + ", Response: " + responseBody);
        }
    }

    private HttpRequest buildRequest(Document document, String signature) {
        String json = convertToJson(document);
        String requestBody = String.format("{\"product_document\": %s, \"document_format\": \"MANUAL\", \"type\": \"LP_INTRODUCE_GOODS\", \"signature\": \"%s\"}", json, signature);
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private String convertToJson(Document document) {
        Gson gson = new Gson();
        return gson.toJson(document);
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}