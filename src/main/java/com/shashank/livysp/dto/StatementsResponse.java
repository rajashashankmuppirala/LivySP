package com.shashank.livysp.dto;

import lombok.Data;

@Data
public class StatementsResponse {
    private String id;
    private String state;
    private Output output;
    private double progress;
    private long started;
    private long completed;
}
