package com.cq.socket5;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Socket5Server {

    private List<Socket5Channel> channelList=new ArrayList<>();

    private ServerSocketChannel server;

    private Selector selector;


    private ByteBuffer buf;

    private int bufSize=2*1024*1024;

    private ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    public Socket5Server() throws IOException {
        buf=ByteBuffer.allocate(bufSize);
        server=ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(1080));
        server.configureBlocking(false);
        selector=Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);

        while (selector.select()>0){
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while(it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if(key.isAcceptable()) {
                    accept(key);
                }
                if(key.isReadable()){
                    try {
                        read(key);
                    }catch (Exception e){

                    }
                }
                if(key.isWritable()){
                    write(key);
                }








            }


        }




    }


    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.configureBlocking(false);
        Socket5Channel socket5Channel=new Socket5Channel(clientChannel.getRemoteAddress());
        clientChannel.register(selector, SelectionKey.OP_READ,socket5Channel);
        channelList.add(socket5Channel);
        System.out.println("a new client connected "+clientChannel.getRemoteAddress());
    }



    private void write(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel)key.channel();
        writeBuffer.clear();
        Socket5Channel channel= (Socket5Channel) key.attachment();
        byte[] send=channel.getSend();
        writeBuffer.put(send);
        socketChannel.write(writeBuffer);
        writeBuffer.flip();
        System.out.println("写入 "+socketChannel.getRemoteAddress() + " "+Arrays.toString(send)+"");
        socketChannel.register(selector,SelectionKey.OP_READ,channel);

    }

    private void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel)key.channel();
        readBuffer.clear();
        int numRead=socketChannel.read(readBuffer);
        readBuffer.flip();
        if(numRead==-1){
            System.out.println("未读入数据"+socketChannel.getRemoteAddress());
            key.cancel();
            socketChannel.close();
        }
        System.out.println("读入 "+socketChannel.getRemoteAddress() + " "+Arrays.toString(readBuffer.array())+"");
        Socket5Channel channel= (Socket5Channel) key.attachment();

        channel.read(readBuffer);
        if(!channel.isClose())socketChannel.register(selector,SelectionKey.OP_WRITE,channel);

    }






}
