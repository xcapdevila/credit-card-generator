package com.github.xcapdevila.creditcard.generator;

import org.springframework.stereotype.Component;

/**
 * @author Xavier Capdevila Estevez on 27/5/21.
 */
@Component
public class LuhnAlgorithmValidator {

  private static final int LUHN_LAST_DIGIT = 9;
  private static final int LUHN_MULTIPLIER = 2;
  private static final long LUHN_MODULE = 10L;

  public boolean isValid(final String pan) {
    int digits = pan.length();
    int oddOrEven = digits & 1;
    long sum = 0L;

    for (int count = 0; count < digits; ++count) {
      int digit;
      try {
        digit = Integer.parseInt(String.valueOf(pan.charAt(count)));
      } catch (NumberFormatException exception) {
        throw new RuntimeException(exception);
      }

      if (((count & 1) ^ oddOrEven) == 0) {
        digit *= LUHN_MULTIPLIER;
        if (digit > LUHN_LAST_DIGIT) {
          digit -= LUHN_LAST_DIGIT;
        }
      }

      sum += digit;
    }

    return sum != 0L && sum % LUHN_MODULE == 0L;
  }

}
