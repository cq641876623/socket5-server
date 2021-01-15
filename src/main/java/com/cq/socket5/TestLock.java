package com.cq.socket5;

/**
 * @author chenqi
 * @date 2020/12/28 13:55
 */
public class TestLock {

    static class A{
        int a=0;
        int b=1;
    }


    public static void main(String[] args) throws InterruptedException {
           A a=new A();
           synchronized (a){
               a.wait();

           }


    }



}
