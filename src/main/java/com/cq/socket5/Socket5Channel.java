package com.cq.socket5;

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






    public Socket5Channel read(byte[] msg){
        if(!isinit){
            if(msg[0] == SOCKS_PROTOCOL_5){

               byte method=msg[2];
               if(  0x02 == msg[1]){
                   method=0x00;;
               }
               this.send=new byte[]{SOCKS_PROTOCOL_5, (byte) method};
                isinit=true;
               if(method==0x00)
               isAuthorized=true;

           }
        }


        return this;

    }

    public byte[] getSend() {
        return send;
    }
}
