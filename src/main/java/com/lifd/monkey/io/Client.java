package com.lifd.monkey.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author lifengdi
 * @createTime 2025/2/7 20:01
 */
public class Client {
    public static void main(String[] args) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 8080));
            while (!socketChannel.finishConnect()) {
                System.out.println("正在连接");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
