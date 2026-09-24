package com.colegio.shuji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ShujiBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(ShujiBackendApplication.class, args);
  }
}
