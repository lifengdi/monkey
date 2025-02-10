package com.lifd.monkey.netty.demo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lifengdi
 * @createTime 2025/2/10 11:16
 */
@ChannelHandler.Sharable
@Slf4j
public class NettyEchoServerHandler extends ChannelInboundHandlerAdapter {
    public static final NettyEchoServerHandler INSTANCE = new NettyEchoServerHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        log.info("msg type: {}", in.hasArray() ? "堆内存" : "直接内存");

        int len = in.readableBytes();
        byte[] arr = new byte[len];
        in.getBytes(0, arr);
        log.info("server received: {}", new String(arr, "UTF-8"));

        //写回数据，异步任务
        log.info("写回前，msg.refCnt:{}", ((ByteBuf) msg).refCnt());
        ChannelFuture f = ctx.writeAndFlush(msg);
        f.addListener((ChannelFuture futureListener) -> {
            log.info("写回后，msg.refCnt:{}", ((ByteBuf) msg).refCnt());
        });
    }
}
