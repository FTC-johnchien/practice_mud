package com.example.htmlmud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.example.htmlmud.infra.gui.MudGuiLauncher;
import javafx.application.Application;

@SpringBootApplication
@EnableScheduling
public class HtmlmudApplication {

  public static void main(String[] args) {
    // SpringApplication.run(HtmlmudApplication.class, args);

    // 1. 啟動 Spring 並取得 Context
    ConfigurableApplicationContext context = SpringApplication.run(HtmlmudApplication.class, args);

    // 列印所有註冊的 Servlet
    String[] servlets = context.getBeanNamesForType(ServletRegistrationBean.class);
    System.out.println("=== 已註冊的 Servlet ===");
    for (String s : servlets)
      System.out.println(s);

    // 2. 將 Context 傳給 JavaFX
    MudGuiLauncher.setSpringContext(context);

    // 3. 啟動 JavaFX 視窗
    Application.launch(MudGuiLauncher.class, args);
    // 使用一個獨立執行緒啟動 JavaFX，避免阻塞 Main 執行緒可能產生的副作用
    // new Thread(() -> {
    // Application.launch(MudGuiLauncher.class, args);
    // }).start();
  }

}
