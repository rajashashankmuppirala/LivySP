package com.shashank.livysp.client;

import com.shashank.livysp.constant.LivyConstants;
import com.shashank.livysp.dto.Session;
import com.shashank.livysp.dto.SessionsResponse;
import com.shashank.livysp.dto.StatementsRequest;
import com.shashank.livysp.dto.StatementsResponse;
import com.shashank.livysp.dto.SessionConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LivyApi {
    private String url;
    private RestTemplate restTemplate;
    public String NAME_PREFIX = "pool";
    public final int CREATE_SESSION_TIMEOUT = 10 * 30;
    public static final int STATEMENT_QUERY_TIMEOUT = 60 * 60;

    public LivyApi(String livyUrl, RestTemplate restTemplate) {
        this.url = livyUrl;
        this.restTemplate = restTemplate;
    }

    public SessionsResponse listPoolSessions() {
        return restTemplate.getForEntity(url + "/sessions", SessionsResponse.class).getBody();
    }

    public Session createSession(SessionConfig sessionConfig) throws IOException {
        sessionConfig.setName(NAME_PREFIX+"-"+ UUID.randomUUID().toString());
        Session initSession = restTemplate.postForEntity(URI.create(url + "/sessions"),sessionConfig,Session.class).getBody();
        StopWatch stopWatch = StopWatch.createStarted();
        while(true) {
            try {
                Thread.sleep(3000);
                long elapsedTime = stopWatch.getTime(TimeUnit.SECONDS);
                if (elapsedTime > CREATE_SESSION_TIMEOUT) {
                    deleteSession(initSession.getId());
                    throw new IOException("Timeout during create session");
                }
                Session currentCreatedSession = getSession(initSession.getId());
                if (StringUtils.equals(currentCreatedSession.getState(), LivyConstants.SESSION_STARTING)) {
                    continue;
                } else if (StringUtils.equals(currentCreatedSession.getState(), LivyConstants.SESSION_IDLE)) {
                    currentCreatedSession.setLivyApi(this);
                    return currentCreatedSession;
                } else {
                    deleteSession(initSession.getId());
                    break;
                }

            } catch (InterruptedException | IOException e) {
                throw new IOException("Create session timeout", e);
            }
        }
        return null;
    }

    public Session getSession(String sessionId){
        return restTemplate.getForEntity(url + "/sessions/" + sessionId, Session.class).getBody();
    }

    public void deleteSession(String sessionId) {
         restTemplate.delete(url + "/sessions/" + sessionId);
    }

    public StatementsResponse executeAsyncStatement(StatementsRequest statementsRequest, String sessionId) {
       return executeStatement(statementsRequest, sessionId);
    }

    public StatementsResponse executeSyncStatement(StatementsRequest statementsRequest, String sessionId) throws SQLException {
        StatementsResponse statementsResponse = executeStatement(statementsRequest, sessionId);
        StopWatch stopWatch = StopWatch.createStarted();
        while(true) {
            StatementsResponse statementResponsestatus = getStatementStatus(statementsResponse.getId(), sessionId);
            if (!StringUtils.equals(statementResponsestatus.getState(), LivyConstants.STATEMENT_RUNNING) &&
                    !StringUtils.equals(statementResponsestatus.getState(), LivyConstants.STATEMENT_WAITING)) {
                return statementResponsestatus;
            }

            long elapsedTime = stopWatch.getTime(TimeUnit.SECONDS);
            if (elapsedTime > STATEMENT_QUERY_TIMEOUT) {
                log.error("execute statement timeout,livy session id:{},statement id:{},code:{}", sessionId, statementsResponse.getId(), statementsRequest.getCode());
                throw new SQLException("query timeout(s) : " + STATEMENT_QUERY_TIMEOUT);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }

    }

    private StatementsResponse executeStatement(StatementsRequest statementsRequest, String sessionId){
        return restTemplate.postForEntity(URI.create(url + "/sessions/" + sessionId + "/statements"),statementsRequest,StatementsResponse.class).getBody();
    }

    private StatementsResponse getStatementStatus(String statementId,String sessionId) {
        return restTemplate.getForEntity(URI.create(url + "/sessions/" + sessionId + "/statements/" + statementId),StatementsResponse.class).getBody();
    }
}
