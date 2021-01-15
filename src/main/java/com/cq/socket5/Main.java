package com.cq.socket5;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;

public class Main {
    public static void main(String[] args)  {
        try {
            Socket5Server socket5= new Socket5Server();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
