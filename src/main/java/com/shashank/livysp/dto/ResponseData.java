package com.shashank.livysp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResponseData {

    @JsonProperty("text/plain")
    private String textPlain;
}
