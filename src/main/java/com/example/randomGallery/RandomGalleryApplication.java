package com.example.randomGallery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RandomGalleryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RandomGalleryApplication.class, args);
    }

}
