package com.cq.socket5;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;

public class Main {
    public static void main(String[] args)  {

        try {
            Socket5Server socket5= new Socket5Server();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
