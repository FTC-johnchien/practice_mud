package com.example.htmlmud.application.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // 只能放在類別上
@Retention(RetentionPolicy.RUNTIME) // 執行時期透過反射讀取
public @interface CommandAlias {
  String[] value(); // 允許傳入多個別名，例如 {"l", "see"}
}
