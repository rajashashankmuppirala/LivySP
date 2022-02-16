package com.shashank.livysp.client;

import com.shashank.livysp.constant.LivyConstants;
import com.shashank.livysp.dto.Session;
import com.shashank.livysp.dto.SessionsResponse;
import com.shashank.livysp.dto.StatementsRequest;
import com.shashank.livysp.dto.StatementsResponse;
import com.shashank.livysp.dto.SessionConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class LivyApi {
    private String url;
    private RestTemplate restTemplate;
    public String NAME_PREFIX = "pool";
    public final int CREATE_SESSION_TIMEOUT = 10 * 30;

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

    public StatementsResponse executeStatement(StatementsRequest statementsRequest, String sessionId) {
       return restTemplate.postForEntity(URI.create(url + "/sessions/" + sessionId + "/statements"),statementsRequest,StatementsResponse.class).getBody();

    }

}
