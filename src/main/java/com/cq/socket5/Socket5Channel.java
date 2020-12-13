package com.cq.socket5;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

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
                execCmd(buf,socketChannel);
                break;
            case Socket5Status.PROXY_REQUEST:
                proxyRequest(buf,socketChannel,proxyType,key);
                break;


        }







        return this;

    }

    private void proxyRequest(ByteBuffer buf, SocketChannel socketChannel, int proxyType,SelectionKey key) {
        switch (proxyType){
            case 0x01:
                if(resultTmp instanceof Socket){
                    try {

                        OutputStream dstOut=((Socket) resultTmp).getOutputStream();
                        buf.clear();
                        int len=-1;
                        while ( 0 !=(len=socketChannel.read(buf))){
                            buf.flip();
                            byte[] buffer=new byte[buf.limit()];
                            buf.get(buffer);
                            System.out.println("请求："+new String(buffer,"UTF-8"));
                            dstOut.write(buffer);
                            dstOut.flush();
                            buf.clear();
                        }
                        buf.clear();
//                        new Thread(()->{
//                            try {
                                proxyReponse(buf, key);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }).start();

                        type=-1;
                        reponseReady=true;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }


    public boolean proxyReponse(ByteBuffer buf,SelectionKey key) throws IOException {
        byte[] readBuffer=new byte[1024];
        boolean isclose=false;
        SocketChannel socketChannel = (SocketChannel)key.channel();
        InputStream dstIn=((Socket) resultTmp).getInputStream();
        int len=-1;
        while ((len=dstIn.read(readBuffer))>0){
            System.out.println("转发结果：  " +new String(readBuffer));
            buf.clear();
            buf.put(readBuffer,0,len);
            buf.flip();
            socketChannel.write(buf);
        }
//        key.cancel();
//        socketChannel.shutdownOutput();
//        socketChannel.close();



        System.out.println("=============请求结束=============");
        isclose=true;

        return isclose;

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

    private void execCmd(ByteBuffer buf,SocketChannel socketChannel) throws IOException {
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
                        resultTmp = new Socket(dstRemoteAddress.getAddress(),dstRemoteAddress.getPort());
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
}
