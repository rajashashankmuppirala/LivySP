package com.shashank.livysp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatementsRequest {
    private String code;
    private String kind;
}
