package org.revcloud.vader.runner.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.revcloud.vader.matchers.AnyMatchers.anyOf;
import static sample.consumer.failure.ValidationFailure.INVALID_COMBO_1;
import static sample.consumer.failure.ValidationFailure.INVALID_COMBO_2;
import static sample.consumer.failure.ValidationFailure.getFailureWithParams;
import static sample.consumer.failure.ValidationFailureMessage.MSG_WITH_PARAMS;

import io.vavr.collection.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.revcloud.vader.runner.Vader;
import org.revcloud.vader.runner.ValidationConfig;
import org.revcloud.vader.specs.Spec;
import org.revcloud.vader.specs.Specs;
import sample.consumer.failure.ValidationFailure;

class Spec2Test {

  @Test
  void spec2Test() {
    final var invalidComboSpec = "invalidComboSpec";
    final var validComboMap =
        Map.of(
            1, Set.of("1", "one"),
            2, Set.of("2", "two"));
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .withSpec(
                spec ->
                    spec._2()
                        .nameForTest(invalidComboSpec)
                        .when(Bean::getValue)
                        .then(Bean::getValueStr)
                        .shouldRelateWith(validComboMap)
                        .orFailWithFn(
                            (fieldName, fieldValue) ->
                                getFailureWithParams(MSG_WITH_PARAMS, fieldName, fieldValue)))
            .prepare();

    final var invalidBean1 = new Bean(1, "a", null, null);
    assertFalse(
        validationConfig
            .getPredicateOfSpecForTest(invalidComboSpec)
            .map(spec -> spec.test(invalidBean1))
            .orElse(true));

    final var invalidBean2 = new Bean(2, "b", null, null);
    assertFalse(
        validationConfig
            .getPredicateOfSpecForTest(invalidComboSpec)
            .map(spec -> spec.test(invalidBean2))
            .orElse(true));

    final var validBean1 = new Bean(1, "one", null, null);
    Assertions.assertTrue(
        validationConfig
            .getPredicateOfSpecForTest(invalidComboSpec)
            .map(spec -> spec.test(validBean1))
            .orElse(false));

    final var validBean2 = new Bean(2, "two", null, null);
    Assertions.assertTrue(
        validationConfig
            .getPredicateOfSpecForTest(invalidComboSpec)
            .map(spec -> spec.test(validBean2))
            .orElse(false));
  }

  @Test
  void spec2TestWithNullValue() {
    final var invalidComboSpec = "invalidComboSpec";
    final var validComboMap =
        Map.of(
            BillingTerm.OneTime, HashSet.of(null, 1).toJavaSet(),
            BillingTerm.Month, Set.of(2));
    final Spec<Bean2, ValidationFailure> bean2Spec =
        spec ->
            spec._2()
                .nameForTest(invalidComboSpec)
                .when(Bean2::getBt)
                .then(Bean2::getValueStr)
                .shouldRelateWith(validComboMap)
                .orFailWithFn(
                    (fieldName, fieldValue) ->
                        getFailureWithParams(MSG_WITH_PARAMS, fieldName, fieldValue));
    final var validationConfig =
        ValidationConfig.<Bean2, ValidationFailure>toValidate().withSpec(bean2Spec).prepare();
    final var validBean = new Bean2(BillingTerm.OneTime, null);
    Assertions.assertTrue(
        validationConfig
            .getPredicateOfSpecForTest(invalidComboSpec)
            .map(spec -> spec.test(validBean))
            .orElse(false));

    final var inValidBean = new Bean2(BillingTerm.Month, null);
    assertFalse(
        validationConfig
            .getPredicateOfSpecForTest(invalidComboSpec)
            .map(spec -> spec.test(inValidBean))
            .orElse(true));
  }

  @DisplayName("More than one Spec 2")
  @Test
  void multiSpec2Test() {
    final Specs<Bean, ValidationFailure> specs =
        spec ->
            List.of(
                spec.<Integer, String>_2()
                    .when(Bean::getValue)
                    .matches(is(1))
                    .then(Bean::getValueStr)
                    .shouldMatch(anyOf("1", "one"))
                    .orFailWith(INVALID_COMBO_1),
                spec.<Integer, String>_2()
                    .when(Bean::getValue)
                    .matches(is(2))
                    .then(Bean::getValueStr)
                    .shouldMatch(either(is("two")).or(is("2")))
                    .orFailWith(INVALID_COMBO_2));
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate().specify(specs).prepare();

    final var invalidBean1 = new Bean(1, "a", null, null);
    final var failureResult1 = Vader.validateAndFailFast(invalidBean1, validationConfig);
    assertThat(failureResult1).contains(INVALID_COMBO_1);

    final var invalidBean2 = new Bean(2, "b", null, null);
    final var failureResult2 = Vader.validateAndFailFast(invalidBean2, validationConfig);
    assertThat(failureResult2).contains(INVALID_COMBO_2);

    final var validBean1 = new Bean(1, "one", null, null);
    final var noneResult1 = Vader.validateAndFailFast(validBean1, validationConfig);
    assertThat(noneResult1).isEmpty();

    final var validBean2 = new Bean(2, "two", null, null);
    final var noneResult2 = Vader.validateAndFailFast(validBean2, validationConfig);
    assertThat(noneResult2).isEmpty();
  }

  @DisplayName("shouldRelateWith OR ShouldRelateWithFn")
  @Test
  void spec2Test2() {
    final var relateWith =
        Map.of(
            1, Set.of("1", "one"),
            2, Set.of("2", "two"));
    final Spec<Bean, ValidationFailure> beanSpec =
        spec ->
            spec.<Integer, String>_2()
                .when(Bean::getValue)
                .then(Bean::getValueStr)
                .shouldRelateWith(relateWith)
                .shouldRelateWithFn((when, then) -> String.valueOf(when).equalsIgnoreCase(then))
                .orFailWith(INVALID_COMBO_1);
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate().withSpec(beanSpec).prepare();
    final var invalidBean1 = new Bean(1, "a", null, null);
    final var failureResult1 = Vader.validateAndFailFast(invalidBean1, validationConfig);
    assertThat(failureResult1).contains(INVALID_COMBO_1);

    final var invalidBean2 = new Bean(1, "one", null, null);
    final var failureResult2 = Vader.validateAndFailFast(invalidBean2, validationConfig);
    assertThat(failureResult2).isEmpty();

    final var invalidBean3 = new Bean(1, "1", null, null);
    final var failureResult3 = Vader.validateAndFailFast(invalidBean3, validationConfig);
    assertThat(failureResult3).isEmpty();
  }

  @DisplayName(
      "Invalid Config: Provide both `when-matches/matchesAnyOf + then-shouldMatch/shouldMatchAnyOf` and `shouldRelateWith` or `shouldRelateWithFn")
  @Test
  void invalidSpec2Config() {
    final var relateWith =
        Map.of(
            1, Set.of("1", "one"),
            2, Set.of("2", "two"));
    final var invalidSpec2Config = "invalidSpec2Config";
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .withSpec(
                spec ->
                    spec.<Integer, String>_2()
                        .nameForTest(invalidSpec2Config)
                        .when(Bean::getValue)
                        .matches(is(1))
                        .then(Bean::getValueStr)
                        .shouldRelateWith(relateWith)
                        .shouldRelateWithFn(
                            (when, then) -> String.valueOf(when).equalsIgnoreCase(then))
                        .orFailWith(INVALID_COMBO_1))
            .prepare();
    final var specWithName = validationConfig.getPredicateOfSpecForTest(invalidSpec2Config);
    assertThrows(IllegalArgumentException.class, () -> specWithName.map(p -> p.test(null)));
  }

  @Test
  void spec2WithName() {
    final var invalidCombo1 = "invalidCombo1";
    final var invalidCombo2 = "invalidCombo2";
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .specify(
                spec ->
                    List.of(
                        spec.<Integer, String>_2()
                            .nameForTest(invalidCombo1)
                            .orFailWith(INVALID_COMBO_1)
                            .when(Bean::getValue)
                            .matches(is(1))
                            .then(Bean::getValueStr)
                            .shouldMatch(either(is("one")).or(is("1"))),
                        spec.<Integer, String>_2()
                            .nameForTest(invalidCombo2)
                            .orFailWith(INVALID_COMBO_2)
                            .when(Bean::getValue)
                            .matches(is(2))
                            .then(Bean::getValueStr)
                            .shouldMatch(either(is("two")).or(is("2")))))
            .prepare();
    final var invalidBean1 = new Bean(1, "a", null, null);
    assertFalse(
        validationConfig
            .getPredicateOfSpecForTest(invalidCombo1)
            .map(spec -> spec.test(invalidBean1))
            .orElse(true));

    final var invalidBean2 = new Bean(2, "b", null, null);
    assertFalse(
        validationConfig
            .getPredicateOfSpecForTest(invalidCombo2)
            .map(spec -> spec.test(invalidBean2))
            .orElse(true));

    final var validBean1 = new Bean(1, "one", null, null);
    Assertions.assertTrue(
        validationConfig
            .getPredicateOfSpecForTest(invalidCombo1)
            .map(spec -> spec.test(validBean1))
            .orElse(false));

    final var validBean2 = new Bean(2, "two", null, null);
    Assertions.assertTrue(
        validationConfig
            .getPredicateOfSpecForTest(invalidCombo2)
            .map(spec -> spec.test(validBean2))
            .orElse(false));
  }

  private enum BillingTerm {
    OneTime,
    Month
  }

  @Value
  private static class Bean {

    Integer value;
    String valueStr;
    Integer dependentValue1;
    Integer dependentValue2;
  }

  @Value
  private static class Bean2 {

    BillingTerm bt;
    String valueStr;
  }
}
