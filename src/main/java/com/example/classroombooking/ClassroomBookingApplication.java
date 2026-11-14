package com.example.classroombooking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.classroombooking.mapper")
@EnableScheduling
public class ClassroomBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassroomBookingApplication.class, args);
    }

}