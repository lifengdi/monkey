package com.lifd.monkey.reactor;

import com.lifd.monkey.config.NioDemoConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lifengdi
 * @createTime 2025/2/8 16:23
 */
@Slf4j
public class MultiThreadEchoServerReactor {
    ServerSocketChannel serverSocket;
    AtomicInteger next = new AtomicInteger(0);
    //selectors 集合,引入多个 selector 选择器
    Selector bossSelector;
    Selector[] workSelectors = new Selector[2];
    //引入多个子反应器
    Reactor[] workReactors = null;
    Reactor bossReactor;

    MultiThreadEchoServerReactor() throws IOException {
        //初始化多个 selector 选择器
        bossSelector = Selector.open();// 用于监听新连接事件
        workSelectors[0] = Selector.open(); // 用于监听 read、write 事件
        workSelectors[1] = Selector.open(); // 用于监听 read、write 事件
        serverSocket = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(NioDemoConfig.SOCKET_SERVER_IP,
                        NioDemoConfig.SOCKET_SERVER_PORT);
        serverSocket.socket().bind(address);
        serverSocket.configureBlocking(false);//非阻塞
        //bossSelector,负责监控新连接事件, 将 serverSocket 注册到 bossSelector
        SelectionKey sk = serverSocket.register(bossSelector, SelectionKey.OP_ACCEPT);
        //绑定 Handler：新连接监控 handler 绑定到 SelectionKey（选择键）
        sk.attach(new AcceptorHandler());
        //bossReactor 反应器，处理新连接的 bossSelector
        bossReactor = new Reactor(bossSelector);
        //第一个子反应器，一子反应器负责一个 worker 选择器
        Reactor workReactor1 = new Reactor(workSelectors[0]);
        //第二个子反应器，一子反应器负责一个 worker 选择器
        Reactor workReactor2 = new Reactor(workSelectors[1]);
        workReactors = new Reactor[]{workReactor1, workReactor2};

    }

    private void startService() {
        // 一子反应器对应一条线程
        new Thread(bossReactor).start();
        new Thread(workReactors[0]).start();
        new Thread(workReactors[1]).start();
        log.info("服务器启动成功");
    }

    //反应器
    class Reactor implements Runnable {
        //每条线程负责一个选择器的查询
        final Selector selector;

        public Reactor(Selector selector) {
            this.selector = selector;
        }

        public void run() {
            try {
                while (!Thread.interrupted()) {
                    //单位为毫秒
                    selector.select(1000);
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    if (null == selectedKeys || selectedKeys.size() == 0) {
                        continue;
                    }
                    Iterator<SelectionKey> it = selectedKeys.iterator();
                    while (it.hasNext()) {
                        //Reactor 负责 dispatch 收到的事件
                        SelectionKey sk = it.next();
                        dispatch(sk);
                    }
                    selectedKeys.clear();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        void dispatch(SelectionKey sk) {
            Runnable handler = (Runnable) sk.attachment();
            //调用之前 attach 绑定到选择键的 handler 处理器对象
            if (handler != null) {
                handler.run();
            }
        }
    }

    public class AcceptorHandler implements Runnable {

        public void run() {
            try {
                SocketChannel channel = serverSocket.accept();
                log.info("接收到一个新的连接");
                if (channel != null) {
                    int index = next.get();
                    log.info("选择器的编号：" + index);
                    Selector selector = workSelectors[index];
                    new MultiThreadEchoHandler(selector, channel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (next.incrementAndGet() == workSelectors.length) {
                next.set(0);
            }
        }
    }

    class MultiThreadEchoHandler implements Runnable {
        final SocketChannel channel;
        final SelectionKey sk;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        static final int RECIEVING = 0, SENDING = 1;
        int state = RECIEVING;
        //引入线程池
        static ExecutorService pool = Executors.newFixedThreadPool(4);

        MultiThreadEchoHandler(Selector selector, SocketChannel c) throws
                IOException {
            channel = c;
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            //仅仅取得选择键，后设置感兴趣的 IO 事件
            sk = channel.register(selector, 0);
            //将本 Handler 作为 sk 选择键的附件，方便事件 dispatch
            sk.attach(this);
            //向 sk 选择键注册 Read 就绪事件
            sk.interestOps(SelectionKey.OP_READ);
            //唤醒选择，使得 OP_READ 生效
            selector.wakeup();
            log.info("新的连接 注册完成");
        }

        public void run() {
            //异步任务，在独立的线程池中执行
            //提交数据传输任务到线程池
            //使得 IO 处理不在 IO 事件轮询线程中执行，在独立的线程池中执行
            pool.execute(new AsyncTask());
        }

        //异步任务，不在 Reactor 线程中执行
        //数据传输与业务处理任务，不在 IO 事件轮询线程中执行，在独立的线程池中执行
        public synchronized void asyncRun() {
            try {
                if (state == SENDING) {
                    //写入通道
                    channel.write(byteBuffer);
                    //写完后,准备开始从通道读,byteBuffer 切换成写模式
                    byteBuffer.clear();
                    //写完后,注册 read 就绪事件
                    sk.interestOps(SelectionKey.OP_READ);
                    //写完后,进入接收的状态
                    state = RECIEVING;
                } else if (state == RECIEVING) {
                    //从通道读
                    int length = 0;
                    while ((length = channel.read(byteBuffer)) > 0) {
                        log.info("客户端消息：" + new String(byteBuffer.array(), 0, length));
                    }
                    //读完后，准备开始写入通道,byteBuffer 切换成读模式
                    byteBuffer.flip();
                    //读完后，注册 write 就绪事件
                    sk.interestOps(SelectionKey.OP_WRITE);
                    //读完后,进入发送的状态
                    state = SENDING;
                }
                //处理结束了, 这里不能关闭 select key，需要重复使用
                //sk.cancel();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        //异步任务的内部类
        class AsyncTask implements Runnable {
            public void run() {
                MultiThreadEchoHandler.this.asyncRun();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        MultiThreadEchoServerReactor multiThreadEchoServerReactor = new MultiThreadEchoServerReactor();
        multiThreadEchoServerReactor.startService();
    }
}
