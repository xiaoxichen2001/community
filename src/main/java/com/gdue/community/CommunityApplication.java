package com.gdue.community;

import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CommunityApplication {

    //解决netty启动冲突问题 （redis和elasticsearch）
    // see Netty4Utils.setAvailableProcessors()
    @Pointcut
    public void init(){
        System.setProperty("es.set.netty.runtime.available.processors","false");
    }

    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }

}
