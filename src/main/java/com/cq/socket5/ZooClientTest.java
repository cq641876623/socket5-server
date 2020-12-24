package com.cq.socket5;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.cq.socket5.ZooTest.contectString;
import static com.cq.socket5.ZooTest.sessionTimeout;

/**
 * @author chenqi
 * @date 2020/12/24 11:35
 */
public class ZooClientTest {

    private static ZooKeeper zooClient;

    private static LinkedList<String> hosts=new LinkedList<>();
    public static void getChildren() throws KeeperException, InterruptedException {

        List<String> hosts=new ArrayList<>();
        List<String> paths=zooClient.getChildren("/servers",true);
        for(String path :paths){
            byte[] data=zooClient.getData("/servers/"+path,false,null);
            hosts.add(new String(data));

        }
        System.out.println(hosts);
        System.out.println("================================");


    }

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {


        zooClient=new ZooKeeper(contectString, sessionTimeout, new Watcher() {
            public void process(WatchedEvent watchedEvent) {
                Date dNow = new Date( );
                SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");
                System.out.println(ft.format(dNow)+"  |  "+watchedEvent.getType());
                try {
                    getChildren();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        getChildren();

        Thread.sleep(Long.MAX_VALUE);
    }

}
