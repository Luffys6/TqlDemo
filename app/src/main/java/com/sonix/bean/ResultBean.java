package com.sonix.bean;

/**
 * create by: LiuShiQi 刘士奇
 * create date: 2018/6/23
 * description:
 */
public class ResultBean {
    private String response;
    private String error;
    private String next;
    private String message;

    @Override
    public String toString() {
        return "ResultBean{" +
                "response='" + response + '\'' +
                ", error='" + error + '\'' +
                ", next='" + next + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
