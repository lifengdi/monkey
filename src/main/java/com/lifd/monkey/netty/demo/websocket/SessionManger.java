package com.lifd.monkey.netty.demo.websocket;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lifengdi
 * @createTime 2025/2/12 08:57
 */
@Slf4j
@Data
@Repository("SessionManger")
public class SessionManger {

    private static SessionManger singleInstance = new SessionManger();

    private ConcurrentHashMap<String, LocalSession> sessionMap = new ConcurrentHashMap<>();

    @Data
    public static class LocalSession {
        private String sessionId;
        private Channel channel;

        public static final AttributeKey<LocalSession> SESSION_KEY =
                AttributeKey.valueOf("SESSION_KEY");

        public LocalSession(String sessionId, Channel channel) {
            this.sessionId = sessionId;
            this.channel = channel;
        }

        public LocalSession bind() {
            log.info(" LocalSession 绑定会话 " + channel.remoteAddress());
            channel.attr(LocalSession.SESSION_KEY).set(this);
            return this;
        }
    }


    public static SessionManger inst()
    {
        return singleInstance;
    }

    public static void setSingleInstance(SessionManger singleInstance)
    {
        SessionManger.singleInstance = singleInstance;
    }

    public void addLocalSession(LocalSession session) {
        //step1: 保存本地的session 到会话清单
        String sessionId = session.getSessionId();
        sessionMap.put(sessionId, session);
    }

    public List<LocalSession> getAllSession() {
        return sessionMap.values().stream().toList();
    }
}
