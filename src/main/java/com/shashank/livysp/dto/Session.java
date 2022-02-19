package com.shashank.livysp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shashank.livysp.client.LivyApi;
import lombok.Data;

import java.sql.SQLException;

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

    public StatementsResponse executeAsycStatement(StatementsRequest statementsRequest) {
        return livyApi.executeAsyncStatement(statementsRequest, id);
    }

    public StatementsResponse executeSyncStatement(StatementsRequest statementsRequest) throws SQLException {
        return livyApi.executeSyncStatement(statementsRequest, id);
    }
}
