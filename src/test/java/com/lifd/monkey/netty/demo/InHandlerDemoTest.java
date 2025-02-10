package com.lifd.monkey.netty.demo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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

    @Test
    public void testWriteRead() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(9, 100);
        System.out.println("动作：分配 ByteBuf(9, 100)" + buffer);
        buffer.writeBytes(new byte[]{1, 2, 3, 4});
        System.out.println("动作：写入 4 个字节 (1,2,3,4)"+ buffer);
        System.out.println("start==========:get==========");
        getByteBuf(buffer);
        System.out.println("动作：取数据 ByteBuf"+ buffer);
        System.out.println("start==========:read==========");
        readByteBuf(buffer);
        System.out.println("动作：读完 ByteBuf"+ buffer);
    }
    //取字节
    private void readByteBuf(ByteBuf buffer) {
        while (buffer.isReadable()) {
            System.out.println("取一个字节:" + buffer.readByte());
        }
    }
    //读字节，不改变指针
    private void getByteBuf(ByteBuf buffer) {
        for (int i = 0; i<buffer.readableBytes(); i++) {
            System.out.println("读一个字节:" + buffer.getByte(i));
        }
    }

}