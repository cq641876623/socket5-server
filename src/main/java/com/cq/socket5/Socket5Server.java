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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Socket5Server {


//    private List<Socket5Channel> channelList=new ArrayList<>();

    private ServerSocketChannel server;

    public static Selector selector;


    private ByteBuffer buf;

    private int bufSize=2*1024*1024;

    private ByteBuffer readBuffer = ByteBuffer.allocate(bufSize);

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

                if(!key.isValid()){
                    key.cancel();
                    key.cancel();
                    key.channel().close();

                }
                if(key.isAcceptable()) {
                    accept(key);
                }




                if(key.isReadable()){
                    try {
                        read(key);
                    }catch (Exception e){
                        e.printStackTrace();
                        key.cancel();
                        key.channel().close();
                    }
                }









            }


        }




    }


    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.socket().setSoTimeout(3000);
        clientChannel.configureBlocking(false);
        Socket5Channel socket5Channel=new Socket5Channel(clientChannel.getRemoteAddress());
        clientChannel.register(selector, SelectionKey.OP_READ,socket5Channel);
        System.out.println("a new client connected "+clientChannel.getRemoteAddress());
    }


//
//    private void write(SelectionKey key) throws IOException{
//        SocketChannel socketChannel = (SocketChannel)key.channel();
//        writeBuffer.clear();
//        Socket5Channel channel= (Socket5Channel) key.attachment();
////        if(channel.getType()==Socket5Status.PROXY_REQUEST&&channel.isReponseReady()){
////            if(!channel.proxyReponse(writeBuffer,key)){
////                socketChannel.register(selector,SelectionKey.OP_WRITE,channel);
////            }
////        }else {
//            byte[] send=channel.getSend();
//            writeBuffer.clear();
//            writeBuffer.put(send);
//            //将缓冲区各标志复位,因为向里面put了数据标志被改变要想从中读取数据发向服务器,就要复位
//            writeBuffer.flip();
//            socketChannel.write(writeBuffer);
//            System.out.println("写入 "+socketChannel.getRemoteAddress() + " "+Arrays.toString(send)+"");
//            socketChannel.register(selector,SelectionKey.OP_READ,channel);
////        }
//
//
//
//    }

    private void read(SelectionKey key) throws IOException{


        SocketChannel socketChannel = (SocketChannel)key.channel();

        Socket5Channel channel= (Socket5Channel) key.attachment();

        channel.read(readBuffer,key);





    }






}
