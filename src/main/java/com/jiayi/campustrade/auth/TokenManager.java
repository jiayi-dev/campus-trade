package com.jiayi.campustrade.auth;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenManager {

    /**
     * Token 有效期：12小时
     */
    private static final long EXPIRE_TIME = 12 * 60 * 60 * 1000L;

    /**
     * 保存 Token 对应的登录会话
     *
     * key：Token
     * value：SessionInfo（用户ID + 过期时间）
     */
    private final ConcurrentHashMap<String, SessionInfo> tokenMap
            = new ConcurrentHashMap<>();

    /**
     * 创建 Token
     */
    public String createToken(Integer userId) {

        // 生成随机 Token
        String token = UUID.randomUUID()
                .toString()
                .replace("-", "");

        // 计算 Token 过期时间
        long expireTime =
                System.currentTimeMillis() + EXPIRE_TIME;

        // 保存 Token 与用户身份的对应关系
        SessionInfo sessionInfo =
                new SessionInfo(userId, expireTime);

        tokenMap.put(token, sessionInfo);

        return token;
    }

    /**
     * 根据 Token 获取用户ID
     */
    public Integer getUserId(String token) {

        // 没有 Token
        if (token == null) {
            return null;
        }

        // 查询 Token 对应的登录会话
        SessionInfo sessionInfo =
                tokenMap.get(token);

        // Token 不存在
        if (sessionInfo == null) {
            return null;
        }

        // Token 已经过期
        if (sessionInfo.getExpireTime()
                < System.currentTimeMillis()) {

            // 惰性删除：发现过期时再删除
            tokenMap.remove(token);

            return null;
        }

        // Token 有效，返回用户ID
        return sessionInfo.getUserId();
    }

    /**
     * 删除 Token
     */
    public void remove(String token) {
        tokenMap.remove(token);
    }

    /**
     * Token 对应的登录会话信息
     */
    private static class SessionInfo {

        private final Integer userId;

        private final long expireTime;

        public SessionInfo(Integer userId, long expireTime) {
            this.userId = userId;
            this.expireTime = expireTime;
        }

        public Integer getUserId() {
            return userId;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }
}