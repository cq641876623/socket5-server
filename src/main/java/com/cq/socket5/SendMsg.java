package com.cq.socket5;

import java.util.Arrays;

public class SendMsg {
    private Integer type;
    private byte[] msg;


    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public byte[] getMsg() {
        return msg;
    }

    public void setMsg(byte[] msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "SendMsg{" +
                "type=" + type +
                ", msg=" + Arrays.toString(msg) +
                '}';
    }
}
