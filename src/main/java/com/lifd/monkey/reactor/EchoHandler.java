package com.lifd.monkey.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author lifengdi
 * @createTime 2025/2/8 16:30
 */
public class EchoHandler implements Runnable {
    final SocketChannel channel;
    final SelectionKey sk;

    EchoHandler(Selector selector, SocketChannel c) {
        try {
            channel = c;
            c.configureBlocking(false);
            //与之前的注册方式不同，先仅仅取得选择键，之后再单独设置感兴趣的 IO 事件
            sk = channel.register(selector, 0); //仅仅取得选择键
            //将 Handler 处理器作为选择键的附件
            sk.attach(this);
            //注册读写就绪事件
            sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        //...处理输入和输出
    }
}
