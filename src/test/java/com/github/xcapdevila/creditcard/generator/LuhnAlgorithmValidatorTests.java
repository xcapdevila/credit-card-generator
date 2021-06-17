package com.github.xcapdevila.creditcard.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LuhnAlgorithmValidatorTests {

  private static final String VALID_PAN = "340184587300287";
  private static final String INVALID_PAN = "340184587300283";
  private LuhnAlgorithmValidator luhnAlgorithmValidator;

  @BeforeEach
  public void setup() {
    luhnAlgorithmValidator = new LuhnAlgorithmValidator();
  }

  @Test
  void givenAValidCreditCardLuhnIsValid() {
    Assertions.assertTrue(luhnAlgorithmValidator.isValid(VALID_PAN));
  }

  @Test
  void givenAnInvalidCreditCardLuhnIsInvalid() {
    Assertions.assertFalse(luhnAlgorithmValidator.isValid(INVALID_PAN));
  }

}
