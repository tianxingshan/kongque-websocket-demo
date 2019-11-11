package com.kongque.ws;


import com.kongque.entity.Message;
import com.kongque.entity.MessageDecoder;
import com.kongque.entity.MessageEncoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/b", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
@Component
@Slf4j
public class TestWebSocket {

    private Logger log = LoggerFactory.getLogger(TestWebSocket.class);

    /**
     * 静态变量，用来记录当前客服人员在线连接数。应该把它设计成线程安全的。
     */
    private static int official = 0;
    /**
     * 同上 客户
     */
    private static int customer = 0;
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
     */
    private static ConcurrentHashMap<String, TestWebSocket> webSocketMapA = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, TestWebSocket> webSocketMapB = new ConcurrentHashMap<>();
    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 当前发消息的人员编号
     */
    private String userId = "";
    /**
     * 当前发消息的人对应的开票员
     */
    private java.lang.String receiveId = "";

    private static synchronized void addA() {
        official++;
    }

    private static synchronized void addB() {
        customer++;
    }

    private static synchronized void subA() {
        official--;
    }

    private static synchronized void subB() {
        customer--;
    }

    private static synchronized int getA() {
        return official;
    }

    private static synchronized int getB() {
        return customer;
    }

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "role") String role) {
        this.session = session;
        userId = UUID.randomUUID().toString();
        // 在线数加1,a代表客服
        if ("a".equals(role)) {
            // 客服的id为数字，这样就可以区分客服和用户了，这里假设不会重复
            // 这里随机的用户id，所以不方便重连，正式环境大概要和用户表结合
            addA();
            webSocketMapA.put(userId, this);
        } else {
            userId = UUID.randomUUID().toString();
            addB();
            webSocketMapB.put(userId, this);
        }
        // 这里可以加一个判断，如果webSocketMap.containsKey(id)，则系统重新指定一个id
        log.info("新连接: " + userId + "----当前客服人数：" + getA() + "----当前客户人数：" + getB());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        // 从map中删除
        if (webSocketMapA.containsKey(userId)) {
            webSocketMapA.remove(userId);
            // 在线数减1
            subA();
            log.info("客服下线:" + userId + "----当前客服人数：" + getA() + "----当前客户人数：" + getB());
        } else {
            webSocketMapB.remove(userId);
            subB();
            log.info("客户下线:" + userId + "----当前客服人数：" + getA() + "----当前客户人数：" + getB());
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(Message message, @PathParam(value = "role") String role) {
        System.out.println(message);
        if ("a".equals(role)) {
            receiveId = message.getCustomer();
            if (message.getCustomer() == null || !webSocketMapB.containsKey(receiveId)) {
                webSocketMapA.get(userId).sendMessage("客户已下线");
            } else {
                webSocketMapB.get(receiveId).sendObj(message);
            }
        } else {
            if (message.getOfficial() != null) {
                if (webSocketMapA.containsKey(message.getOfficial())) {
                    webSocketMapA.get(message.getOfficial()).sendObj(message);
                } else {
                    webSocketMapB.get(userId).sendMessage("当前客服已下线，请换个客服人员重新咨询");
                }
            } else {
                if (webSocketMapA.size() == 0) {
                    webSocketMapB.get(userId).sendMessage("当前无在线客服");
                } else {
                    // 系统随机指定客服，正式环境应当判断客服接应人数然后再进行指配
                    int i = new Random().nextInt(webSocketMapA.size());
                    message.setOfficial(webSocketMapA.keySet().toArray(new String[0])[i]);
                    message.setCustomer(userId);
                    webSocketMapA.get(message.getOfficial()).sendObj(message);
                }
            }
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Throwable error) {
        log.info(error.toString());
    }

    /**
     * 发送对象到客户端
     */
    private void sendObj(Message message) {
        try {
            this.session.getBasicRemote().sendObject(message);
        } catch (EncodeException | IOException e) {
            log.info("错误：由用户" + userId + "向" + receiveId + message.toString() + "具体错误为：" + e.toString());
        }
    }

    /**
     * 发送文本到客户端
     */
    private void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.info("错误：由用户" + userId + "向" + receiveId + message + "具体错误为：" + e.toString());
        }
    }
}
