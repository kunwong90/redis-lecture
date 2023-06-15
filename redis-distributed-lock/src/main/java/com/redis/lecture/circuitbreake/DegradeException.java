package com.redis.lecture.circuitbreake;

public class DegradeException extends RuntimeException {

    public DegradeException(String msg) {
        super(msg);
    }
}
