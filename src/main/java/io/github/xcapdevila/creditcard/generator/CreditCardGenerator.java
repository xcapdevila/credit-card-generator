package io.github.xcapdevila.creditcard.generator;

import com.github.curiousoddman.rgxgen.RgxGen;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
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

  private static final Pattern NOW_REGEX = Pattern.compile("\\$\\{now}");
  private static final Pattern PAN_REGEX = Pattern.compile("\\$\\{pan}");
  private static final Pattern CVV_REGEX = Pattern.compile("\\$\\{cvv}");
  private static final Pattern EXP_DATE_REGEX = Pattern.compile("\\$\\{expDate}");
  private static final Pattern ISSUER_NAME_REGEX = Pattern.compile("\\$\\{issuerName}");

  private final CreditCardGeneratorConfiguration creditCardGeneratorConfiguration;
  private final LuhnAlgorithmValidator luhnAlgorithmValidator;
  private final FileService fileService;

  public Path generateRandomCardsToFile() throws IOException {
    log.info("Generating random cards...");
    val cards = generateRandomCards();

    log.info("Getting filename...");
    val filename = getDateBasedFilename();
    log.info("Writing to file...");
    final Path filePath = fileService.write(cards, filename);
    log.info("Generated cards to file successful");
    return filePath;
  }

  private String getDateBasedFilename() {
    val nowStr = LocalDateTime.now().toString();
    val nowFilename = nowStr
        .substring(0, nowStr.indexOf("."))
        .replaceAll("-", "_")
        .replaceFirst("T", "_")
        .replaceAll(":", "_");
    return NOW_REGEX.matcher(creditCardGeneratorConfiguration.getOutputFile()).replaceAll(nowFilename);
  }

  public Set<String> generateRandomCards() {
    val cards = new HashSet<String>();
    log.info("Card generator process starting...");
    for (CreditCardGeneratorConfiguration.CreditCardIssuer creditCardIssuer : creditCardGeneratorConfiguration.getIssuers()) {
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
            String cardInfo = creditCardGeneratorConfiguration.getOutputPattern();
            cardInfo = PAN_REGEX.matcher(cardInfo).replaceAll(pan);
            cardInfo = CVV_REGEX.matcher(cardInfo).replaceAll(cvvGenerator.generate());
            cardInfo = EXP_DATE_REGEX.matcher(cardInfo).replaceAll(expDateGenerator.generate());
            cardInfo = ISSUER_NAME_REGEX.matcher(cardInfo).replaceAll(creditCardIssuer.getName());

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
