package io.github.wasiliystrecker.returns;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringArchitecturePatternsApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringArchitecturePatternsApplication.class, args);
  }

  @Bean
  Clock returnWorkflowClock() {
    return Clock.systemUTC();
  }
}
