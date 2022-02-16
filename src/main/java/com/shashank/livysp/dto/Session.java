package com.shashank.livysp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shashank.livysp.client.LivyApi;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Session {
    private String id;
    private String name;
    private String appId;
    private String owner;
    private String proxyUser;
    private String kind;
    private String state;
    private LivyApi livyApi;

    public StatementsResponse executeStatement(StatementsRequest statementsRequest) {
        return livyApi.executeStatement(statementsRequest, id);
    }

}
