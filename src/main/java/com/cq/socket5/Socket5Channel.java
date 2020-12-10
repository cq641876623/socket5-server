package com.cq.socket5;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Socket5Channel {

    private static final int SOCKS_PROTOCOL_4 = 0X04;
    private static final int SOCKS_PROTOCOL_5 = 0X05;
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final byte TYPE_IPV4 = 0x01;
    private static final byte TYPE_IPV6 = 0X02;
    private static final byte TYPE_HOST = 0X03;
    private static final byte ALLOW_PROXY = 0X5A;
    private static final byte DENY_PROXY = 0X5B;

    private boolean isinit;

    private User user;

    private boolean isAuthorized;

    private byte[] send;

    private boolean isClose;

    private SocketAddress address;


    public boolean isClose() {
        return isClose;
    }

    public Socket5Channel(SocketAddress address) {
        this.address=address;
        this.isClose = false;
    }

    public void setClose(boolean close) {
        isClose = close;
    }

    public Socket5Channel read(ByteBuffer buf){


//        判断是否进行初始化握手
        if(!isinit&&buf.limit()==3){
            init(buf);
            System.out.println(address+" 进行初始化完毕");
            return this;
        }
        if(isinit&&isAuthorized){
            execCmd(buf);
            return this;
        }




        return this;

    }


    private void init(ByteBuffer buf){
//            判断是否为SOCKET5协议
        if( buf.get(0) == SOCKS_PROTOCOL_5){

            byte method=buf.get(2);
            if(  0x02 == buf.get(1)){
                method=0x00;;
            }
            this.send=new byte[]{SOCKS_PROTOCOL_5, (byte) method};
            isinit=true;
//               当验证身份方法为无需验证时放行
            if(method==0x00)isAuthorized=true;

        }
    }

    private void execCmd(ByteBuffer buf){
        if(isSocket5(buf)){
            int cmd=buf.get(1);
            int atyp=buf.get(3);
            String host=null;
            host=getHost(buf,atyp);


            switch (cmd){

            }

        }
    }

    private String getHost(ByteBuffer buf,int atyp) {
        String host="";
        byte[] tmp;
        switch (atyp){
            case TYPE_IPV4:
                tmp = new byte[4];
                buf.get(tmp,4,4);
                try {
                    host = InetAddress.getByAddress(tmp).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case TYPE_IPV6:
                tmp = new byte[6];
                buf.get(tmp,4,6);
                try {
                    host = InetAddress.getByAddress(tmp).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case TYPE_HOST:
                tmp=new byte[buf.limit()-6];
                buf.get(tmp,4,tmp.length);
                host=new String(tmp);
                break;
        }


        return host;
    }

    private boolean isSocket5(ByteBuffer buf){
        return buf.get(0)==SOCKS_PROTOCOL_5;

    }


    public byte[] getSend() {
        return send;
    }
}
