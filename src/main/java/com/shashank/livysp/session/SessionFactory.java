package com.shashank.livysp.session;

import com.shashank.livysp.client.LivyApi;
import com.shashank.livysp.constant.LivyConstants;
import com.shashank.livysp.dto.Session;
import com.shashank.livysp.dto.SessionConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class SessionFactory extends BasePooledObjectFactory<Session> {
    private LivyApi livyApi;
    private LinkedList<Session> existingPoolSessions;
    private SessionConfig sessionConfig;

    public SessionFactory(String livyUrl, SessionConfig sessionConfig, RestTemplate restTemplate) {
         this.sessionConfig = sessionConfig;
         if(null == restTemplate){
             restTemplate = new RestTemplate();
             restTemplate.getInterceptors().add((request, body, execution) ->{
                 request.getHeaders().add("X-Requested-By", "LivyClient");
                 request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                 return execution.execute(request, body);
             });
         }
         this.livyApi = new LivyApi(livyUrl,restTemplate);
         checkForExistingPoolSessions();

    }

    private void checkForExistingPoolSessions() {
        try {
            List<Session> sessionList = livyApi.listPoolSessions().getSessions().stream()
                    .filter(session -> null!= session && null!= session.getName()
                            && session.getName().startsWith(this.livyApi.NAME_PREFIX))
                    .map(session -> {
                        session.setLivyApi(livyApi);
                        return session;
                    }).collect(Collectors.toList());
            this.existingPoolSessions = new LinkedList<>(sessionList);
        } catch (Exception e) {
            log.error("init livy session error : {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Session create() throws Exception {
        synchronized (this) {
            if (CollectionUtils.isNotEmpty(existingPoolSessions)) {
                return existingPoolSessions.poll();
            }
        }
        return livyApi.createSession(sessionConfig);
    }

    @Override
    public PooledObject<Session> wrap(Session session) {
        return new DefaultPooledObject<>(session);
    }

    @Override
    public void destroyObject(PooledObject<Session> p) throws Exception {
        super.destroyObject(p);
        livyApi.deleteSession(p.getObject().getId());
    }

    @Override
    public boolean validateObject(PooledObject<Session> p) {
        Session session = livyApi.getSession(p.getObject().getId());
        if (session == null) {
            log.error("validate livy session error:session does not exist, id:{}", p.getObject().getId());
            return false;
        }
        String sessionState = session.getState();
        if (StringUtils.equals(sessionState, LivyConstants.SESSION_IDLE) ||
                StringUtils.equals(sessionState, LivyConstants.SESSION_BUSY) ||
                StringUtils.equals(sessionState, LivyConstants.SESSION_SUCCESS)) {
            log.debug("validate livy session,id:{},state:true", p.getObject().getId());
            return true;
        } else if (StringUtils.equals(sessionState, LivyConstants.SESSION_STARTING)) {
            StopWatch stopWatch = StopWatch.createStarted();
            while (true) {
                try {
                    Thread.sleep(3000);
                    log.info("waiting 3000 ms for creating session");
                    long elapsedTime = stopWatch.getTime(TimeUnit.SECONDS);
                    if (elapsedTime > livyApi.CREATE_SESSION_TIMEOUT) {
                         log.info("create session timeout(s) : " + livyApi.CREATE_SESSION_TIMEOUT);
                         livyApi.deleteSession(session.getId());
                         log.error("validate livy session failure,create session timeout,id:{}", p.getObject().getId());
                        return false;
                    }
                    Session currentSession = livyApi.getSession(session.getId());
                    if (StringUtils.equals(currentSession.getState(), LivyConstants.SESSION_IDLE)) {
                        log.debug("validate livy session success,id:{}", p.getObject().getId());
                        return true;
                    } else if (StringUtils.equals(currentSession.getState(), LivyConstants.SESSION_STARTING)) {
                        continue;
                    } else {
                        livyApi.deleteSession(currentSession.getId());
                        log.error("validate livy session error,id:{},state:{}", p.getObject().getId(), currentSession.getState());
                        return false;
                    }
                } catch (Exception e) {
                    //throw new IOException("error while validating object")
                }
            }
        }
        return false;
    }
}
