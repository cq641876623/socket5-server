package com.cq.socket5.NioServer;

import java.net.Socket;

/**
 * @author chenqi
 * @date 2020/12/28 14:27
 */
public class Server {



    private static final int SOCKS_PROTOCOL_4 = 0X04;
    private static final int SOCKS_PROTOCOL_5 = 0X05;
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final byte TYPE_IPV4 = 0x01;
    private static final byte TYPE_IPV6 = 0X02;
    private static final byte TYPE_HOST = 0X03;
    private static final byte ALLOW_PROXY = 0X5A;
    private static final byte DENY_PROXY = 0X5B;
    private Socket sourceSocket;



}
