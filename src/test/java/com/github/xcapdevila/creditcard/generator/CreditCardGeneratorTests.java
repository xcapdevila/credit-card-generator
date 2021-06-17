package com.github.xcapdevila.creditcard.generator;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.xcapdevila.creditcard.generator.CreditCardGeneratorConfiguration.CreditCardIssuer;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.val;
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

  private static final String DATE_TIME_PLACEHOLDER = "${now}";
  private static final String PAN_PLACEHOLDER = "${pan}";
  private static final String CVV_PLACEHOLDER = "${cvv}";
  private static final String EXP_DATE_PLACEHOLDER = "${expDate}";
  private static final String ISSUER_NAME_PLACEHOLDER = "${issuerName}";
  private static final List<String> OUTPUT_PATTERN_KEYWORDS =
      asList(PAN_PLACEHOLDER, CVV_PLACEHOLDER, EXP_DATE_PLACEHOLDER, ISSUER_NAME_PLACEHOLDER);
  private static final String DELIMITER = ",";
  
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
    creditCardGeneratorConfiguration.setOutputFile("test_cards_" + DATE_TIME_PLACEHOLDER + ".csv");
    creditCardGeneratorConfiguration.setOutputPattern(String.join(DELIMITER, OUTPUT_PATTERN_KEYWORDS));
    val panRegex = "^4[0-9]{15}$";
    val cvvRegex = "^[0-9]{3}$";
    val expDateRegex = "^(0[1-9]|1[0-2])(2[2-7])$";
    val creditCardIssuers = new ArrayList<CreditCardIssuer>();
    val creditCardIssuerLuhn = new CreditCardIssuer();
    creditCardIssuerLuhn.setCards(10);
    creditCardIssuerLuhn.setName("test_luhn");
    creditCardIssuerLuhn.setPanRegex(panRegex);
    creditCardIssuerLuhn.setCvvRegex(cvvRegex);
    creditCardIssuerLuhn.setExpDateRegex(expDateRegex);
    creditCardIssuerLuhn.setLuhnCompliant(true);
    creditCardIssuers.add(creditCardIssuerLuhn);
    val creditCardIssuerNoLuhn = new CreditCardIssuer();
    creditCardIssuerNoLuhn.setCards(20);
    creditCardIssuerNoLuhn.setName("test_no_luhn");
    creditCardIssuerNoLuhn.setPanRegex(panRegex);
    creditCardIssuerNoLuhn.setCvvRegex(cvvRegex);
    creditCardIssuerNoLuhn.setExpDateRegex(expDateRegex);
    creditCardIssuerNoLuhn.setLuhnCompliant(false);
    creditCardIssuers.add(creditCardIssuerNoLuhn);
    creditCardGeneratorConfiguration.setIssuers(creditCardIssuers);

    luhnAlgorithmValidator = new LuhnAlgorithmValidator();
  }

  @Test
  void givenAValidConfigCardsAreGeneratedAndWrittenToFile() throws IOException {
    val path = Paths.get("pom.xml");
    when(fileService.write(any(Set.class), any(String.class))).thenReturn(path);

    assertFalse(creditCardGeneratorConfiguration.getIssuers().isEmpty());

    val now = LocalDateTime.now();

    val creditCardGenerator = new CreditCardGenerator(creditCardGeneratorConfiguration, luhnAlgorithmValidator, fileService);
    assertEquals(path, creditCardGenerator.generateRandomCardsToFile());

    Mockito.verify(fileService).write(cardsCaptor.capture(), filenameCaptor.capture());

    val cards = cardsCaptor.getValue();
    val expectedCards = creditCardGeneratorConfiguration.getIssuers().
        stream()
        .map(CreditCardIssuer::getCards)
        .reduce(Integer::sum)
        .orElseThrow(RuntimeException::new);

    val outputPattern = creditCardGeneratorConfiguration.getOutputPattern();
    assertAll(
        () -> assertEquals(expectedCards.intValue(), cards.size()),
        () -> {
          for (CreditCardIssuer creditCardIssuer : creditCardGeneratorConfiguration.getIssuers()) {

            val issuerCards = cards
                .stream()
                .filter(card -> !outputPattern.contains(ISSUER_NAME_PLACEHOLDER) || card.contains(creditCardIssuer.getName()))
                .count();

            val issuerCardValues = cards
                .stream()
                .filter(card -> !outputPattern.contains(ISSUER_NAME_PLACEHOLDER) || card.contains(creditCardIssuer.getName()))
                .findAny()
                .orElseThrow(RuntimeException::new)
                .split(DELIMITER);

            val panIdx = OUTPUT_PATTERN_KEYWORDS.indexOf(PAN_PLACEHOLDER);
            val cvvIdx = OUTPUT_PATTERN_KEYWORDS.indexOf(CVV_PLACEHOLDER);
            val expDateIdx = OUTPUT_PATTERN_KEYWORDS.indexOf(EXP_DATE_PLACEHOLDER);
            assertAll(
                () -> assertTrue(!outputPattern.contains(ISSUER_NAME_PLACEHOLDER) || creditCardIssuer.getCards() == issuerCards),
                () -> assertTrue(!outputPattern.contains(PAN_PLACEHOLDER) || Pattern.matches(creditCardIssuer.getPanRegex(), issuerCardValues[panIdx])),
                () -> assertTrue(!outputPattern.contains(CVV_PLACEHOLDER) || Pattern.matches(creditCardIssuer.getCvvRegex(), issuerCardValues[cvvIdx])),
                () -> assertTrue(
                    !outputPattern.contains(EXP_DATE_PLACEHOLDER) || Pattern.matches(creditCardIssuer.getExpDateRegex(), issuerCardValues[expDateIdx])),
                () -> assertTrue(
                    !creditCardIssuer.isLuhnCompliant() || !outputPattern.contains(PAN_PLACEHOLDER) || luhnAlgorithmValidator.isValid(issuerCardValues[panIdx]))
            );
          }
        }
    );

    val filename = filenameCaptor.getValue();
    val outputFile = creditCardGeneratorConfiguration.getOutputFile();
    assertAll(
        () -> assertTrue(filename.startsWith(outputFile.substring(0, outputFile.indexOf(DATE_TIME_PLACEHOLDER)))),
        () -> assertTrue(filename.endsWith(outputFile.substring(outputFile.indexOf(DATE_TIME_PLACEHOLDER) + DATE_TIME_PLACEHOLDER.length()))),
        () -> assertTrue(filename.contains(String.valueOf(now.getYear()))),
        () -> assertTrue(filename.contains(String.valueOf(now.getMonthValue()))),
        () -> assertTrue(filename.contains(String.valueOf(now.getDayOfMonth()))),
        () -> assertTrue(filename.contains(String.valueOf(now.getHour()))),
        () -> assertTrue(filename.contains(String.valueOf(now.getMinute())))
    );

  }

}
