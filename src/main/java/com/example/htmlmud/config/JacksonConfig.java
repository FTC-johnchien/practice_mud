package com.example.htmlmud.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

  @Bean
  @Primary // 確保這是預設使用的 ObjectMapper
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // 1. 忽略 JSON 中存在但 Java POJO 中沒有的欄位 (避免新舊版本不相容導致報錯)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // 2. 允許空的 Bean (某些只有方法的物件序列化時不報錯)
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    // 3. (選用) 排版輸出，方便 Debug 查看 JSON，正式環境可關閉
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    return mapper;
  }
}
