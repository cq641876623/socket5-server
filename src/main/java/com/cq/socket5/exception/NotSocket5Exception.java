package com.cq.socket5.exception;

/**
 * @author chenqi
 * @date 2020/12/11 9:12
 */
public class NotSocket5Exception extends Exception {


    public NotSocket5Exception() {
        super("非socket5协议");
    }
}
