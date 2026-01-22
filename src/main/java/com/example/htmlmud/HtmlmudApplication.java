package com.example.htmlmud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HtmlmudApplication {

  public static void main(String[] args) {
    SpringApplication.run(HtmlmudApplication.class, args);
  }

}
