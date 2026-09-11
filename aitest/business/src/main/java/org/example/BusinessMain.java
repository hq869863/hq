package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author hq
 */
@SpringBootApplication
public class BusinessMain {
    public static void main(String[] args) {
        SpringApplication.run(BusinessMain.class, args);
        System.out.println("=====================BusinessMain");
    }
}