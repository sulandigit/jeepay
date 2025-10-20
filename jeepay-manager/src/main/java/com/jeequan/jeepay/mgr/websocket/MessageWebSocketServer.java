/*
 * Copyright (c) 2021-2031, 河北计全科技有限公司 (https://www.jeequan.com & jeequan@126.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeequan.jeepay.mgr.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket服务端点 - 站内消息推送
 * 
 * @author [mybatis plus generator]
 * @since 2025
 */
@Slf4j
@Component
@ServerEndpoint("/websocket/message/{userId}")
public class MessageWebSocketServer {

    /** 在线连接数 */
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

    /** 存放所有在线客户端，key为用户ID */
    private static final Map<String, Session> CLIENTS = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     * @param session WebSocket会话
     * @param userId 用户ID
     * @since 2025
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        CLIENTS.put(userId, session);
        int count = ONLINE_COUNT.incrementAndGet();
        log.info("新连接接入，用户ID: {}, 当前在线人数: {}", userId, count);
        
        // 发送连接成功消息
        JSONObject message = new JSONObject();
        message.put("type", "connect");
        message.put("message", "连接成功");
        message.put("userId", userId);
        sendMessage(session, message.toJSONString());
    }

    /**
     * 连接关闭调用的方法
     * @param userId 用户ID
     * @since 2025
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        CLIENTS.remove(userId);
        int count = ONLINE_COUNT.decrementAndGet();
        log.info("连接断开，用户ID: {}, 当前在线人数: {}", userId, count);
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送的消息
     * @param userId 用户ID
     * @since 2025
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        log.info("收到用户 {} 的消息: {}", userId, message);
        
        // 处理心跳消息
        if ("ping".equals(message)) {
            Session session = CLIENTS.get(userId);
            if (session != null) {
                sendMessage(session, "pong");
            }
        }
    }

    /**
     * 发生错误时调用
     * @param session WebSocket会话
     * @param error 错误信息
     * @since 2025
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误", error);
    }

    /**
     * 发送消息给指定用户
     * @param userId 用户ID
     * @param message 消息内容
     * @return 是否发送成功
     * @since 2025
     */
    public static boolean sendMessageToUser(String userId, String message) {
        Session session = CLIENTS.get(userId);
        if (session != null && session.isOpen()) {
            return sendMessage(session, message);
        }
        log.warn("用户 {} 不在线，无法发送消息", userId);
        return false;
    }

    /**
     * 发送消息给指定用户(JSON对象)
     * @param userId 用户ID
     * @param message 消息对象
     * @return 是否发送成功
     * @since 2025
     */
    public static boolean sendMessageToUser(String userId, Object message) {
        return sendMessageToUser(userId, JSON.toJSONString(message));
    }

    /**
     * 群发消息
     * @param message 消息内容
     * @since 2025
     */
    public static void sendMessageToAll(String message) {
        CLIENTS.forEach((userId, session) -> {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        });
        log.info("群发消息给 {} 个在线用户", CLIENTS.size());
    }

    /**
     * 群发消息(JSON对象)
     * @param message 消息对象
     * @since 2025
     */
    public static void sendMessageToAll(Object message) {
        sendMessageToAll(JSON.toJSONString(message));
    }

    /**
     * 发送消息的底层方法
     * @param session WebSocket会话
     * @param message 消息内容
     * @return 是否发送成功
     * @since 2025
     */
    private static boolean sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
            return true;
        } catch (IOException e) {
            log.error("发送消息失败", e);
            return false;
        }
    }

    /**
     * 获取当前在线人数
     * @return 在线人数
     * @since 2025
     */
    public static int getOnlineCount() {
        return ONLINE_COUNT.get();
    }

    /**
     * 判断用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     * @since 2025
     */
    public static boolean isUserOnline(String userId) {
        Session session = CLIENTS.get(userId);
        return session != null && session.isOpen();
    }

}
