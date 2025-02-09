package com.lifd.monkey.netty.demo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InHandlerDemoTest {

    @Test
    public void testInHandlerLifeCircle() {
        final InHandlerDemo inHandler = new InHandlerDemo();
        //初始化处理器
        ChannelInitializer i = new ChannelInitializer<EmbeddedChannel>() {
                    protected void initChannel(EmbeddedChannel ch) {
                        ch.pipeline().addLast(inHandler);
                    }
                };
        //创建嵌入式通道
        EmbeddedChannel channel = new EmbeddedChannel(i);
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);
        //模拟入站，向嵌入式通道写一个入站数据包
        channel.writeInbound(buf);
        channel.flush();
        //模拟入站，再写一个入站数据包
        channel.writeInbound(buf);
        channel.flush();
        //通道关闭
        channel.close();
    }

}