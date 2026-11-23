package com.yunzhongyi.medical;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 云中医医务系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.yunzhongyi.medical.**.mapper")
public class MedicalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalApplication.class, args);
    }
}
