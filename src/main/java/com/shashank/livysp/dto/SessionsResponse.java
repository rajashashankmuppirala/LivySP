package com.shashank.livysp.dto;

import lombok.Data;

import java.util.List;

@Data
public class SessionsResponse {
    private int from;
    private int total;
    private List<Session> sessions;
}
