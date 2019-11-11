package com.kongque.entity;

import com.alibaba.fastjson.JSON;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class MessageEncoder implements Encoder.Text<Message>{


    @Override
    public void init(EndpointConfig endpointConfig) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public String encode(Message message) throws EncodeException {
        return String.valueOf(message);
    }
}
