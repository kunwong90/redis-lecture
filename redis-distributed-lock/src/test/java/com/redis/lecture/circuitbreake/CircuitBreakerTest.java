package com.redis.lecture.circuitbreake;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class CircuitBreakerTest {

    @Test
    public void test1() {
        CircuitBreaker circuitBreaker = new CircuitBreaker(new Config());
        IntStream.range(0, 10).forEach(value -> {
            String result = circuitBreaker.run(() -> {
                // 假装执行失败
                System.out.println("执行方法次数:" + value);
                int i = 1 / 0;
                return "Success";
            }, throwable -> {
                System.err.println(throwable);
                return "执行自定义降级方法";
            });


        });
    }

    @Test
    public void test2() {
        CircuitBreaker circuitBreaker = new CircuitBreaker(new Config());
        IntStream.range(0, 10).forEach(value -> {
            String result = circuitBreaker.run(() -> {
                // 假装执行失败
                System.out.println("执行方法次数:" + value);
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int i = 1 / 0;
                return "Success";
            }, throwable -> {
                System.err.println(throwable);
                return "执行自定义降级方法";
            });
        });
    }

    @Test
    public void test3() {
        CircuitBreaker circuitBreaker = new CircuitBreaker(new Config());
        IntStream.range(0, 10).forEach(value -> {
            if (value == 5) {
                try {
                    TimeUnit.MILLISECONDS.sleep(5000 + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String result = circuitBreaker.run(() -> {
                // 假装执行失败
                System.out.println("执行方法次数:" + value);
                if (value < 5) {
                    int i = 1 / 0;
                }
                System.out.println("方法执行成功");
                return "Success";
            }, throwable -> {
                System.err.println(throwable);
                return "执行自定义降级方法";
            });
        });
    }

}