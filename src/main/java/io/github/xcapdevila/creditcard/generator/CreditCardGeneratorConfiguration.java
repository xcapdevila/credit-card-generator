package io.github.xcapdevila.creditcard.generator;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
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

  @NotBlank
  private String outputFile;
  @NotBlank
  private String outputPattern;
  @NotEmpty
  private List<CreditCardIssuer> issuers;

  @Getter
  @Setter
  public static final class CreditCardIssuer {

    @NotBlank
    private String name;
    @NotNull
    private Integer cards;
    @NotBlank
    private String panRegex;
    @NotBlank
    private String cvvRegex;
    @NotBlank
    private String expDateRegex;
    private boolean luhnCompliant;
  }

}
