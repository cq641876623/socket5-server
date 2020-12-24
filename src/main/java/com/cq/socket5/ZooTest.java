package com.cq.socket5;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;

/**
 * @author chenqi
 * @date 2020/12/23 17:13
 */
public class ZooTest {

    public static String contectString="192.168.200.130:2181,192.168.200.131:2181,192.168.200.132:2181,192.168.200.133:2181";

    public static  int sessionTimeout=2000;

    private static ZooKeeper zooClient;

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {

        zooClient=new ZooKeeper(contectString, sessionTimeout, new Watcher() {
            public void process(WatchedEvent watchedEvent) {
                System.out.println(watchedEvent.getType());
            }
        });
        Stat stat=zooClient.exists("/servers",false);
        if(stat==null){
            String path=zooClient.create("/servers","myfirstServer".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
            System.out.println(path);
        }
        zooClient.create("/servers/server",args[0].getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
        Thread.sleep(Long.MAX_VALUE);







    }
}
