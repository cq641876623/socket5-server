package com.cq.socket5;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author chenqi
 * @date 2020/12/30 10:39
 */
public class Session {

    public static ThreadLocal<SimpleDateFormat> sf=new ThreadLocal<>();

    public static ExecutorService treadpool=Executors.newFixedThreadPool(5);
}
