package com.cq.socket5;

import com.cq.socket5.exception.NotSocket5Exception;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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





    public boolean isClose() {
        return type == -1;
    }

    public Socket5Channel(SocketAddress address) {
        this.address=address;
        this.type = 0;
    }



    public Socket5Channel read(ByteBuffer buf, SocketChannel socketChannel)  {

        switch (type){
            case Socket5Status.HANDSHAKE:
//                协议格式长度校验
                init(buf);
                System.out.println(address+" 进行初始化完毕");

                break;
            case Socket5Status.IDENTITY_AUTHENTICATION:
                break;
            case Socket5Status.EXECUTE_THE_ORDER:
                break;


        }



        if(isinit&&isAuthorized){
            execCmd(buf,socketChannel);
            return this;
        }



//        当以上都不执行则表示没匹配到
        this.isClose=true;
        return this;

    }


    private void init(ByteBuffer buf){
//            判断是否为SOCKET5协议
        if( buf.get(0) == SOCKS_PROTOCOL_5){
            int mlen=buf.get(1);
            int method=0xFF;
            byte[] methods=new byte[mlen];
            getByte(buf,methods,2,mlen);



            this.send=new byte[]{SOCKS_PROTOCOL_5, (byte) method};
//               当验证身份方法为无需验证时放行
            if(method==0x00)isAuthorized=true;


        }
    }

    private void execCmd(ByteBuffer buf,SocketChannel socketChannel)  {
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

    public byte[] getSend() {
        return send;
    }
}
