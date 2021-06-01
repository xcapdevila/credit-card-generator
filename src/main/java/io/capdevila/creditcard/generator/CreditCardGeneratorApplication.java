package io.capdevila.creditcard.generator;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Slf4j
@SpringBootApplication
public class CreditCardGeneratorApplication implements CommandLineRunner, ApplicationContextAware {

  @Value("${application.dry-run:false}")
  private boolean dryRun;

  private ApplicationContext applicationContext;

  public static void main(String[] args) {
    SpringApplication.run(CreditCardGeneratorApplication.class, args);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("Application starting...");
    if (dryRun) {
      log.info("Dry run mode activated. Execution skipped.");
    } else {
      val creditCardGenerator = applicationContext.getBean(CreditCardGenerator.class);
      creditCardGenerator.generateRandomCardsToFile();
    }
    log.info("Application shutting down...");
  }

}
