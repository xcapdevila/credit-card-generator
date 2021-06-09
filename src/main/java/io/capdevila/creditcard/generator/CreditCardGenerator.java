package io.capdevila.creditcard.generator;

import com.github.curiousoddman.rgxgen.RgxGen;
import io.capdevila.creditcard.generator.CreditCardGeneratorConfiguration.CreditCardIssuer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

/**
 * @author Xavier Capdevila Estevez on 28/5/21.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCardGenerator {

  private final CreditCardGeneratorConfiguration creditCardGeneratorConfiguration;
  private final LuhnAlgorithmValidator luhnAlgorithmValidator;
  private final FileService fileService;

  public boolean generateRandomCardsToFile() throws IOException {
    log.info("Generating random cards...");
    val cards = generateRandomCards();

    log.info("Getting filename...");
    val filename = getDateBasedFilename();
    log.info("Writing to file...");
    fileService.write(cards, filename);
    log.info("Generated cards written into {}", filename);
    return true;
  }

  private String getDateBasedFilename() {
    val nowStr = LocalDateTime.now().toString();
    val nowFilename = nowStr
        .substring(0, nowStr.indexOf("."))
        .replaceAll("-", "_")
        .replaceFirst("T", "_")
        .replaceAll(":", "_");
    return creditCardGeneratorConfiguration.getOutputFile().replaceAll("\\$\\{now}", nowFilename);
  }

  public Set<String> generateRandomCards() {
    val cards = new HashSet<String>();
    log.info("Card generator process starting...");
    for (CreditCardIssuer creditCardIssuer : creditCardGeneratorConfiguration.getIssuers()) {
      log.info("Generating {} {} cards...", creditCardIssuer.getCards(), creditCardIssuer.getName());

      val panGenerator = new RgxGen(creditCardIssuer.getPanRegex());
      val cvvGenerator = new RgxGen(creditCardIssuer.getCvvRegex());
      val expDateGenerator = new RgxGen(creditCardIssuer.getExpDateRegex());
      for (int i = 0; i < creditCardIssuer.getCards(); ++i) {
        boolean isValid = false;
        String pan;
        while (!isValid) {
          pan = panGenerator.generate();
          isValid = !creditCardIssuer.isLuhnCompliant() || luhnAlgorithmValidator.isValid(pan);
          if (isValid) {
            val cardInfo = String
                .format(creditCardGeneratorConfiguration.getOutputPattern(),
                    pan,
                    cvvGenerator.generate(),
                    expDateGenerator.generate(),
                    creditCardIssuer.getName());
            isValid = cards.add(cardInfo);
            if (log.isDebugEnabled() && isValid) {
              log.debug("Generated card: {}", cardInfo);
            }
          }
        }
      }
      log.info("{} {} cards generated.", creditCardIssuer.getCards(), creditCardIssuer.getName());
    }
    log.info("Card generator process finished.");
    log.info("Total generated cards: {}", cards.size());

    return cards;
  }

}
