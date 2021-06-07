package io.capdevila.creditcard.generator;

import io.capdevila.creditcard.generator.CreditCardGeneratorConfiguration.CreditCardIssuer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditCardGeneratorTests {

  private CreditCardGeneratorConfiguration creditCardGeneratorConfiguration;
  private LuhnAlgorithmValidator luhnAlgorithmValidator;
  @Captor
  private ArgumentCaptor<Set<String>> cardsCaptor;
  @Captor
  private ArgumentCaptor<String> filenameCaptor;
  @Mock
  private FileService fileService;

  @BeforeEach
  public void setup() {
    creditCardGeneratorConfiguration = new CreditCardGeneratorConfiguration();
    // Output file must contain one %s (must be improved)
    val now = LocalDateTime.now();
    creditCardGeneratorConfiguration.setOutputFile("test_cards_${now}.csv");
    // CSV output pattern is coupled to the tests (must be improved)
    creditCardGeneratorConfiguration.setOutputPattern("%s,%s,%s,%s");
    val creditCardIssuers = new ArrayList<CreditCardIssuer>();
    val creditCardIssuerLuhn = new CreditCardIssuer();
    creditCardIssuerLuhn.setCards(10);
    creditCardIssuerLuhn.setName("test_luhn");
    creditCardIssuerLuhn.setPanRegex("^4[0-9]{15}$");
    creditCardIssuerLuhn.setCvvRegex("^[0-9]{3}$");
    creditCardIssuerLuhn.setExpDateRegex("^(0[1-9]|1[0-2])(2[2-7])$");
    creditCardIssuerLuhn.setLuhnCompliant(true);
    creditCardIssuers.add(creditCardIssuerLuhn);
    val creditCardIssuerNoLuhn = new CreditCardIssuer();
    creditCardIssuerNoLuhn.setCards(20);
    creditCardIssuerNoLuhn.setName("test_no_luhn");
    creditCardIssuerNoLuhn.setPanRegex("^4[0-9]{15}$");
    creditCardIssuerNoLuhn.setCvvRegex("^[0-9]{3}$");
    creditCardIssuerNoLuhn.setExpDateRegex("^(0[1-9]|1[0-2])(2[2-7])$");
    creditCardIssuerNoLuhn.setLuhnCompliant(false);
    creditCardIssuers.add(creditCardIssuerNoLuhn);
    creditCardGeneratorConfiguration.setIssuers(creditCardIssuers);

    luhnAlgorithmValidator = new LuhnAlgorithmValidator();
  }

  @Test
  void givenAValidConfigCardsAreGeneratedAndWrittenToFile() throws IOException {
    val now = LocalDateTime.now();

    val creditCardGenerator = new CreditCardGenerator(creditCardGeneratorConfiguration, luhnAlgorithmValidator, fileService);
    Assertions.assertDoesNotThrow(() -> creditCardGenerator.generateRandomCardsToFile());

    Mockito.verify(fileService).write(cardsCaptor.capture(), filenameCaptor.capture());

    val cards = cardsCaptor.getValue();
    val expectedCards = creditCardGeneratorConfiguration.getIssuers().
        stream()
        .map(CreditCardIssuer::getCards)
        .reduce(Integer::sum)
        .orElseThrow(RuntimeException::new);

    Assertions.assertAll(
        () -> Assertions.assertEquals(expectedCards.intValue(), cards.size()),
        () -> {
          for (CreditCardIssuer creditCardIssuer : creditCardGeneratorConfiguration.getIssuers()) {

            val issuerCards = cards
                .stream()
                .filter(card -> card.contains(creditCardIssuer.getName()))
                .count();

            val issuerCardValues = cards
                .stream()
                .filter(card -> card.contains(creditCardIssuer.getName()))
                .findAny()
                .orElseThrow(RuntimeException::new)
                // CSV output pattern is coupled to the tests (must be improved)
                .split(",");

            Assertions.assertAll(

                () -> Assertions.assertTrue(Pattern.matches(creditCardIssuer.getPanRegex(), issuerCardValues[0])),
                () -> Assertions.assertTrue(Pattern.matches(creditCardIssuer.getCvvRegex(), issuerCardValues[1])),
                () -> Assertions.assertTrue(Pattern.matches(creditCardIssuer.getExpDateRegex(), issuerCardValues[2])),
                () -> Assertions.assertTrue(!creditCardIssuer.isLuhnCompliant() || luhnAlgorithmValidator.isValid(issuerCardValues[0]))
            );
          }
        }
    );

    val filename = filenameCaptor.getValue();
    val outputFile = creditCardGeneratorConfiguration.getOutputFile();
    Assertions.assertAll(
        () -> Assertions.assertTrue(filename.startsWith(outputFile.substring(0, outputFile.indexOf("%")))),
        () -> Assertions.assertTrue(filename.endsWith(outputFile.substring(outputFile.indexOf("%")+2))),
        () -> Assertions.assertTrue(filename.contains(String.valueOf(now.getYear()))),
        () -> Assertions.assertTrue(filename.contains(String.valueOf(now.getMonthValue()))),
        () -> Assertions.assertTrue(filename.contains(String.valueOf(now.getDayOfMonth()))),
        () -> Assertions.assertTrue(filename.contains(String.valueOf(now.getHour()))),
        () -> Assertions.assertTrue(filename.contains(String.valueOf(now.getMinute())))
    );

  }

}
