package com.lifd.monkey.reactor;

import com.lifd.monkey.config.NioDemoConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lifengdi
 * @createTime 2025/2/8 14:39
 */
@Slf4j
public class EchoClient {
    public static void startClient() throws IOException {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18899);
        // 1.获取通道
        SocketChannel socketChannel = SocketChannel.open(address);
        // 2.切换成非阻塞模式
        socketChannel.configureBlocking(false);
        //不断地自旋、等待连接完成，或者做一些其他的事情
        while (!socketChannel.finishConnect()) {
        }
        log.info("客户端连接成功");

        //启动接受线程
        Processer processer = new Processer(socketChannel);
        new Thread(processer).start();

    }

    static class Processer implements Runnable {
        final Selector selector;
        final SocketChannel channel;

        static ExecutorService pool = Executors.newFixedThreadPool(4);

        Processer(SocketChannel channel) throws IOException {
            //Reactor初始化
            selector = Selector.open();
            this.channel = channel;
            channel.register(selector,
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }

        public void run() {
            pool.execute(() -> {
                try {
                    while (!Thread.interrupted()) {
                        selector.select();
                        Set<SelectionKey> selected = selector.selectedKeys();
                        Iterator<SelectionKey> it = selected.iterator();
                        while (it.hasNext()) {
                            SelectionKey sk = it.next();
                            if (sk.isWritable()) {
                                ByteBuffer buffer = ByteBuffer.allocate(NioDemoConfig.SEND_BUFFER_SIZE);

                                Scanner scanner = new Scanner(System.in);
                                log.info("请输入发送内容:");
                                if (scanner.hasNext()) {
                                    SocketChannel socketChannel = (SocketChannel) sk.channel();
                                    String next = scanner.next();
                                    String msg = LocalDateTime.now() + " >>" + next;
                                    log.info("UDP 客户端发送数据：" + msg);
                                    buffer.put(msg.getBytes());
                                    buffer.flip();
                                    // 操作三：发送数据
                                    socketChannel.write(buffer);
                                    buffer.clear();
                                }

                            }
                        }
                        selected.clear();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            pool.execute(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(1000);
                        selector.select();
                        Set<SelectionKey> selected = selector.selectedKeys();
                        Iterator<SelectionKey> it = selected.iterator();
                        while (it.hasNext()) {
                            SelectionKey sk = it.next();
                            if (sk.isReadable()) {
                                // 若选择键的IO事件是“可读”事件,读取数据
                                SocketChannel socketChannel = (SocketChannel) sk.channel();

                                //读取数据
                                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                                int length = 0;
                                while ((length = socketChannel.read(byteBuffer)) > 0) {
                                    byteBuffer.flip();
                                    log.info("server echo:" + new String(byteBuffer.array(), 0, length));
                                    byteBuffer.clear();
                                }

                            }
                            //处理结束了, 这里不能关闭select key，需要重复使用
                            //selectionKey.cancel();
                        }
                        selected.clear();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static void main(String[] args) throws IOException {
        startClient();
    }
}
