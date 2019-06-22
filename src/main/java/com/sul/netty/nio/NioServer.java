package com.sul.netty.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioServer {
    private int port;
    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    public NioServer(int port) {
        try {
            this.port = port;
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(port));
            server.configureBlocking(false);

            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void server() throws Exception {
        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                handle(key);
            }
        }
    }

    private void handle(SelectionKey key) throws Exception {
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel)key.channel();
            SocketChannel socket = server.accept();
            socket.configureBlocking(false);
            socket.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            SocketChannel socket = (SocketChannel)key.channel();
            int len = socket.read(buffer);
            if ( len > 0 ) {
                buffer.flip();
                String content = new String(buffer.array(),0,len);
                key = socket.register(selector,SelectionKey.OP_WRITE);
                //在key上携带一个附件，一会再写出去
                key.attach(content);
            }
        } else if (key.isWritable()) {
            SocketChannel socket = (SocketChannel)key.channel();
            String content = (String)key.attachment();
            socket.write(ByteBuffer.wrap(("输出：" + content).getBytes()));
            socket.close();
        }
    }

    public static void main(String[] args) throws Exception {
        new NioServer(8080).server();
    }

}
