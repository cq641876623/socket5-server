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
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Socket5Server {


    private static final int SOCKS_PROTOCOL_4 = 0X04;
    private static final int SOCKS_PROTOCOL_5 = 0X05;
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final byte TYPE_IPV4 = 0x01;
    private static final byte TYPE_IPV6 = 0X02;
    private static final byte TYPE_HOST = 0X03;
    private static final byte ALLOW_PROXY = 0X5A;
    private static final byte DENY_PROXY = 0X5B;



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
                if(key.isWritable()){
                    write(key);
                }
                if(key.isReadable()){
                    read(key);
                }






            }


        }




    }


    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ,1);
        System.out.println("a new client connected "+clientChannel.getRemoteAddress());
    }



    private void write(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel)key.channel();
        writeBuffer.clear();
        SendMsg sendMsg= (SendMsg) key.attachment();
        switch (sendMsg.getType()){
            case 1:
                writeBuffer.put(sendMsg.getMsg());
                writeBuffer.flip();
                socketChannel.write(writeBuffer);
                System.out.println(sendMsg);;
                socketChannel.register(selector,SelectionKey.OP_READ,2);
                break;
            case 2:


                break;

        }

    }

    private void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel)key.channel();
        readBuffer.clear();
        int r=socketChannel.read(readBuffer);
        readBuffer.flip();
        if (1 == (int)key.attachment() && r==3){
           byte[] reTmp;
           reTmp=readBuffer.array();
           int method=0;
           if(reTmp[0] == SOCKS_PROTOCOL_5){
               method=reTmp[2];
               if(  0x02 == reTmp[1]){
                   method=0x00;;
               }
           }

           byte[] msg=new byte[]{SOCKS_PROTOCOL_5, (byte) method};
           SendMsg sendType=new SendMsg();
            sendType.setType(1);
            sendType.setMsg(msg);

           socketChannel.register(selector,SelectionKey.OP_WRITE,sendType);
           return;
        }
        if(2 == (int)key.attachment() ){
            byte[] reTmp;
            reTmp=readBuffer.array();
            SendMsg sendType=new SendMsg();
            sendType.setType(2);
            sendType.setMsg(reTmp);
            if(sendType.getMsg()[0]==SOCKS_PROTOCOL_5 ){
                switch (sendType.getMsg()[1]){
                    default:
                        byte atyp=sendType.getMsg()[3];
                        if(atyp==0x01) {
                            byte[] ipv4=new byte[4];
                            for(int j=4,i=0;i<4 ;i++){
                                ipv4[i]=sendType.getMsg()[j+i];
                            }
                            System.out.println(InetAddress.getByAddress(ipv4).getHostAddress());
                        }
                        break;
                }
            }




            socketChannel.register(selector,SelectionKey.OP_WRITE,sendType);

            return;
        }



    }






}
