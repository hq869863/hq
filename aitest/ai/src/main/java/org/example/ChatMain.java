package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author hq
 */
@SpringBootApplication
public class ChatMain {
    public static void main(String[] args) {
        SpringApplication.run(ChatMain.class, args);
        System.out.println("===========ChatMain");
    }
}