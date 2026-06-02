package com.bisai.dto;

import lombok.Data;

@Data
public class TestModelRequest {
    private String apiUrl;
    private String apiKey;
    private String model;
}
