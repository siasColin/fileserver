package com.colin.springboot.fileserver.model;

/**
 * layUI table表格接收数据json格式
 */
public class LayUI {

    private  int code = -1; //成功失败标示
    private String msg = "未知异常";// 提示信息
    private long count = 1000L; //数据总量
    private Object data;// 数据

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
