package com.example.htmlmud.domain.model.config;

public interface Weighted {
  /**
   * 回傳該物件的權重值 權重越高，被抽中的機率越高
   */
  int getWeight();
}
