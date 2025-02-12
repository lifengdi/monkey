package com.lifd.monkey.netty.demo.websocket;


import com.lifd.monkey.util.Dateutil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame>
{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception
    {
        // ping 和 pong 帧已经被前面的WebSocketServerProtocolHandler处理器处理过了
        if (frame instanceof TextWebSocketFrame)
        {
            // 取得请求内容
            String request = ((TextWebSocketFrame) frame).text();
            log.info("服务端收到：{}", request);
            //回显字符串
            String echo = Dateutil.getTime() + ctx.channel().attr(SessionManger.LocalSession.SESSION_KEY).get().getSessionId()
                    + "：" + request;
            TextWebSocketFrame echoFrame = new TextWebSocketFrame(echo);
            // 发送回显字符串
            ctx.channel().writeAndFlush(echoFrame);
            List<SessionManger.LocalSession> allSession = SessionManger.inst().getAllSession();
            for (SessionManger.LocalSession localSession : allSession)
            {
                if (!localSession.getChannel().equals(ctx.channel())) {
                    TextWebSocketFrame tempFrame = new TextWebSocketFrame(echo);
                    localSession.getChannel().writeAndFlush(tempFrame);
                }
            }
        } else
        {
            log.debug("本例程仅支持文本消息，不支持二进制消息");
            // 如果不是文本消息，抛出异常
            // 本演示不支持二进制消息
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        //添加连接
     }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        //断开连接
     }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        ctx.flush();
    }

    //处理用户事件
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        //是否握手成功，升级为 Websocket 协议
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete)
        {
            // 握手成功，移除 WebPageHandler，因此将不会接收到任何消息
            ctx.pipeline().remove(WebPageHandler.class);
            String sessionId = UUID.randomUUID().toString();
            SessionManger.LocalSession localSession = new SessionManger.LocalSession(sessionId, ctx.channel());
            localSession.bind();
            SessionManger.inst().addLocalSession(localSession);
            log.info("WebSocket HandshakeComplete 握手成功");
            log.info("新的WS客户端加入，通道为：{}", ctx.channel());
        } else
        {
            super.userEventTriggered(ctx, evt);
        }
    }
}