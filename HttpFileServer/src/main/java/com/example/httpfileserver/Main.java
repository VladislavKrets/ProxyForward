package com.example.httpfileserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        FileUtils.setRootDirectory("/home/lollipop");
        System.out.println("utils set");
        SpringApplication.run(Main.class, args);
    }
}
