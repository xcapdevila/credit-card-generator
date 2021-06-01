package io.capdevila.creditcard.generator;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * @author Xavier Capdevila Estevez on 27/5/21.
 */
@Validated
@Component
@ConfigurationProperties("creditcard.generator")
@Getter
@Setter
public class CreditCardGeneratorConfiguration {

  private String outputFile;
  private String outputPattern;
  private List<CreditCardIssuer> issuers;

  @Getter
  @Setter
  public static final class CreditCardIssuer {

    private String name;
    private Integer cards;
    private String panRegex;
    private String cvvRegex;
    private String expDateRegex;
    private boolean luhnCompliant;
  }

}
