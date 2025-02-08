package com.lifd.monkey.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * @author lifengdi
 * @createTime 2025/2/8 11:26
 */
public class Server {
    public void receive() throws IOException {
        //获取 DatagramChannel 数据报通道
        DatagramChannel datagramChannel = DatagramChannel.open();
        //设置为非阻塞模式
        datagramChannel.configureBlocking(false);
        //绑定监听地址
        datagramChannel.bind(new InetSocketAddress("127.0.0.1", 18899));
        System.out.println("UDP 服务器启动成功！");

        //开启一个通道选择器
        Selector selector = Selector.open();

        //将通道注册到选择器
        datagramChannel.register(selector, SelectionKey.OP_READ);

        //通过选择器，查询 IO 事件
        while (selector.select() > 0) {
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            //迭代 IO 事件
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();

                //可读事件，有数据到来
                if (selectionKey.isReadable()) {

                    //读取 DatagramChannel 数据报通道的数据
                    SocketAddress client = datagramChannel.receive(buffer);
                    buffer.flip();
                    System.out.println(new String(buffer.array(), 0, buffer.limit()));
                    buffer.clear();
                }
            }
            iterator.remove();
        }

        //关闭选择器和通道
        selector.close();
        datagramChannel.close();
    }

    public static void main(String[] args) {
        try {
            new Server().receive();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
