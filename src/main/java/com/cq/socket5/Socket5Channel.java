package com.cq.socket5;



import java.io.IOException;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.cq.socket5.Session.sf;
import static com.cq.socket5.Session.treadpool;


public class Socket5Channel {

    public static final byte[] CERTIFICATION_METHOD=new byte[]{0x00,0x02};

    private static final int SOCKS_PROTOCOL_4 = 0X04;
    private static final int SOCKS_PROTOCOL_5 = 0X05;
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final byte TYPE_IPV4 = 0x01;
    private static final byte TYPE_IPV6 = 0X02;
    private static final byte TYPE_HOST = 0X03;
    private static final byte ALLOW_PROXY = 0X5A;
    private static final byte DENY_PROXY = 0X5B;

    public SocketChannel client;
    public SocketChannel remote;


    public String uid;


    private User user;

    private boolean isAuthorized;

    private byte[] send;


    private SocketAddress address;

    private Object resultTmp;

    private InetSocketAddress dstRemoteAddress;

    private int type;

    private int proxyType=-1;

    private boolean reponseReady=false;

    public boolean isReponseReady() {
        return reponseReady;
    }

    public int getType() {
        return type;
    }

    public boolean isClose() {
        return type == -1;
    }

    public Socket5Channel(SocketAddress address) {
        this.address=address;
        this.type = 0;
        this.uid= UUID.randomUUID().toString();
    }



    public Socket5Channel read(ByteBuffer buf, SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel)key.channel();

        switch (type){
            case Socket5Status.HANDSHAKE:
//                协议格式长度校验
                readBuf(buf,socketChannel,key);
                init(buf, socketChannel);
                System.out.println(address+" 进行初始化完毕");

                break;
            case Socket5Status.IDENTITY_AUTHENTICATION:
                break;
            case Socket5Status.EXECUTE_THE_ORDER:
                readBuf(buf,socketChannel,key);
                Socket5Channel that1=this;
                treadpool.submit(()->{
                    try {
                        that1.execCmd(buf,socketChannel);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                });

                break;
            case Socket5Status.PROXY_REQUEST:
                Socket5Channel that=this;
//                treadpool.submit(()->{
//                    try {
                        that.proxyRequest(buf,socketChannel,proxyType,key);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                });






                break;


        }







        return this;

    }


    private synchronized void proxyRequest(ByteBuffer buf, SocketChannel socketChannel, int proxyType,SelectionKey key) throws IOException {
        if(Session.sf.get()==null){
            Session.sf.set(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        }

        Date now=new Date();

        switch (proxyType){
            case 0x01:
                if(resultTmp instanceof SocketChannel){



                    SocketChannel remote= (SocketChannel) resultTmp;


                    if(client.socket().getRemoteSocketAddress()==null || remote==null ){
                        System.out.println(sf.get().format(now)+"  "+socketChannel.getRemoteAddress()+"   ->   "+remote.getRemoteAddress()+"     "+": 该应用已断开连接 因为==null");
                        key.cancel();
                        socketChannel.shutdownOutput();
                        socketChannel.shutdownInput();
                        key.channel().close();
                        return;
                    }

                    if(!socketChannel.isConnected()){
                        System.out.println(sf.get().format(now)+"  "+socketChannel.getRemoteAddress()+"   ->   "+remote.getRemoteAddress()+"     "+this.uid+": 该应用已断开连接");
                        key.cancel();
                        socketChannel.shutdownOutput();
                        socketChannel.shutdownInput();
                        key.channel().close();
                        return;
                    }

                    ByteBuffer buffer=ByteBuffer.allocate(1024);
                    int total=0;
                    String type="";
                    if(socketChannel.socket().getRemoteSocketAddress().equals(remote.socket().getRemoteSocketAddress())){
                        type="远";
                        buffer.clear();
                        int len1=0;
                        while((len1=socketChannel.read(buffer))>0){
                            buffer.flip();
                            client.write(buffer);
                            buffer.clear();
                            total+=len1;
                        }
                        if(len1==-1){
                            System.out.println(sf.get().format(now)+"  "+socketChannel.getRemoteAddress()+"   ->   "+remote.getRemoteAddress()+"     "+this.uid+": 应用关闭");
                            key.cancel();
                            socketChannel.shutdownOutput();
                            socketChannel.shutdownInput();
                            key.channel().close();
                            return;
                        }

                    }
                    if(socketChannel.socket().getRemoteSocketAddress().equals(client.socket().getRemoteSocketAddress())){

                        type="近";

                        buf.clear();
                        int len2=0;

                        while((len2=socketChannel.read(buffer))>0){
                            buffer.flip();
                            remote.write(buffer);
                            buffer.clear();
                            total+=len2;
                        }
                        if(len2==-1){
                            System.out.println(sf.get().format(now)+"  "+socketChannel.getRemoteAddress()+"   ->   "+remote.getRemoteAddress()+"     "+this.uid+": 应用关闭");
                            key.cancel();
                            socketChannel.shutdownOutput();
                            socketChannel.shutdownInput();
                            key.channel().close();
                            return;
                        }
                    }


                    System.out.println(sf.get().format(now)+"  "+socketChannel.getRemoteAddress()+"   ->   "+remote.getRemoteAddress()+"     "+this.uid+": 从"+type+"端读取数据："+total);


                }
                break;
        }
    }





    private void init(ByteBuffer buf,SocketChannel socketChannel) throws IOException {
//            判断是否为SOCKET5协议
        if( buf.get(0) == SOCKS_PROTOCOL_5){
            int mlen=buf.get(1);
            int method=0xFF;
            byte[] methods=new byte[mlen];
            getByte(buf,methods,2,mlen);
            for(int i=0;i<methods.length;i++){
               for(int j=0;j<CERTIFICATION_METHOD.length;j++){
                  if( methods[i]==CERTIFICATION_METHOD[j] ){
                      method=methods[i];
                      break;
                  }

               }
            }

            this.send=new byte[]{SOCKS_PROTOCOL_5, (byte) method};
            buf.clear();
            buf.put(send);
            buf.flip();
            socketChannel.write(buf);
//               当验证身份方法为无需验证时放行
            if(method==0x00){
                isAuthorized=true;
                type=2;
            }else {
                type=1;
            }



        }
    }

    private synchronized void execCmd(ByteBuffer buf,SocketChannel socketChannel) throws IOException {
        if(isSocket5(buf)){
            ByteBuffer rsv = ByteBuffer.allocate(10);
            rsv.put((byte) SOCKS_PROTOCOL_5);
            int cmd=buf.get(1);
            int atyp=buf.get(3);
            String host=null;
            int port=-1;
            host=getHost(buf,atyp);
            port=getPort(buf);
            System.out.println("host: "+host+" : "+port);
            this.dstRemoteAddress=new InetSocketAddress(host,port);
            proxyType=cmd;
            switch (cmd){
                case 0x01:
                    try {
                        this.client=socketChannel;
                        Date now=new Date();
                        long startTime=System.nanoTime();
                        if(Session.sf.get()==null){
                            Session.sf.set(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));}

                            SocketChannel remote = SocketChannel.open(new InetSocketAddress(dstRemoteAddress.getAddress(),dstRemoteAddress.getPort()));
                        long endTime=System.nanoTime();
                        System.out.println(sf.get().format(now)+"  "+socketChannel.getRemoteAddress()+"   ->   "+remote.getRemoteAddress()+"     "+this.uid+": 连接远端读取数据共耗时："+ TimeUnit.NANOSECONDS.toMicros(endTime-startTime) +"ms");
                        resultTmp=remote;
                        remote.configureBlocking(false);
                        remote.register(Socket5Server.selector,SelectionKey.OP_READ,this);
                        rsv.put((byte) 0x00);
                    } catch (IOException e) {
                        e.printStackTrace();
                        rsv.put((byte) 0x05);
                    }
                    break;
                case 0x02:
                    try {
                        resultTmp = new ServerSocket(port);
                    } catch (IOException e) {
                        e.printStackTrace();
                        rsv.put((byte) 0x05);
                    }
                    break;
                case 0x03:
                    try {
                        resultTmp=new DatagramSocket();
                    } catch (SocketException e) {
                        e.printStackTrace();
                        rsv.put((byte) 0x05);
                    }
                    rsv.put((byte) 0x00);
                    break;
            }
            rsv.put((byte) 0x00);
            rsv.put((byte) 0x01);
            rsv.put(socketChannel.socket().getLocalAddress().getAddress());
            Short localPort = (short) ((socketChannel.socket().getLocalPort()) & 0xFFFF);
            rsv.putShort(localPort);
            rsv.flip();
            send=new byte[rsv.limit()];
            rsv.get(send);
            buf.clear();
            buf.put(send);
            buf.flip();
            socketChannel.write(buf);
            type=3;

        }
    }

    private String getHost(ByteBuffer buf,int atyp) {
        String host="";
        byte[] tmp;
        switch (atyp){
            case TYPE_IPV4:
                tmp = new byte[4];
                tmp=getByte(buf,tmp,4,tmp.length);
                try {
                    host = InetAddress.getByAddress(tmp).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case TYPE_IPV6:
                tmp = new byte[6];
                tmp=getByte(buf,tmp,4,tmp.length);
                try {
                    host = InetAddress.getByAddress(tmp).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case TYPE_HOST:
                tmp=new byte[buf.get(4)];
                tmp=getByte(buf,tmp,5,tmp.length);
                host=new String(tmp);
                break;
        }


        return host;
    }
    private int getPort(ByteBuffer buf) {
       int port=-1;
       byte[] portBytes=new byte[2];
        getByte(buf,portBytes,buf.limit()-2,portBytes.length);
//        byte temp=portBytes[0];
//        portBytes[0]=portBytes[1];
//        portBytes[1]=temp;
        port=ByteBuffer.wrap(portBytes).asShortBuffer().get() & 0xFFFF;
        return port;
    }

    private boolean isSocket5(ByteBuffer buf){
        return buf.get(0)==SOCKS_PROTOCOL_5;

    }


    public byte[] getByte(ByteBuffer buf,byte[] dst,int offset,int length){
        for (int i = offset,j=0; i < offset + length; i++){
            dst[j++] = buf.get(i);
        }
        return dst;
    }


    private void readBuf(ByteBuffer buf,SocketChannel socketChannel,SelectionKey key) throws IOException {
        buf.clear();
        int numRead=socketChannel.read(buf);
        buf.flip();
        if(numRead==-1){
            System.out.println("未读入数据"+socketChannel.getRemoteAddress());
            key.cancel();
            socketChannel.close();
        }
    }

    public static String AsciiToString(byte[] result2) {
        StringBuilder sbu = new StringBuilder();
        for (byte b : result2) {
            if (0 == b) {
                break;
            }
            sbu.append((char) Integer.parseInt(String.valueOf(b)));
        }
        return sbu.toString();
    }
}
