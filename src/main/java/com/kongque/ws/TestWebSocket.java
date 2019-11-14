package com.kongque.ws;


import com.kongque.entity.Message;
import com.kongque.entity.MessageDecoder;
import com.kongque.entity.MessageEncoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/{role}", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
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

    /*
    客服加1
     */
    private static synchronized void addA() {
        official++;
    }

    /*
    用户加1
     */
    private static synchronized void addB() {
        customer++;
    }

    /*
    客服减1
     */
    private static synchronized void subA() {
        official--;
    }

    /*
    用户减1
     */
    private static synchronized void subB() {
        customer--;
    }

    /*
    获取客服人数
     */
    private static synchronized int getA() {
        return official;
    }

    /*
    获取用户
     */
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
        if ("a".equals(role)) {//客服连接
            addA();
            webSocketMapA.put("userA_Id", this);
        } else {//客户廉价
            userId = UUID.randomUUID().toString();
            addB();
            webSocketMapB.put("userB_Id", this);
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
        log.info("websocket关闭连接");
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(Message message, @PathParam(value = "role") String role) {
        log.info("onmessage监听成功");
        System.out.println(message.getContent());
        if ("a".equals(role)) {//客服
            TestWebSocket testWebSocket = webSocketMapB.get("userB_Id");
            webSocketMapB.get("userB_Id").sendMessage(message.getContent());
           log.info("客服向A客户发送消息 : " + message.getContent());

        } else {//用户
            TestWebSocket testWebSocket = webSocketMapA.get("userA_Id");
            webSocketMapA.get("userA_Id").sendMessage(message.getContent());
            log.info("客户向客服发送消息 : " + message.getContent());
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
            log.info("服务器向客户端发送消息开始");
            RemoteEndpoint.Basic basicRemote = this.session.getBasicRemote();
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
