package org.revcloud.vader.runner.config;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.revcloud.vader.runner.Vader.validateAndFailFast;
import static sample.consumer.failure.ValidationFailure.FIELD_INTEGRITY_EXCEPTION;
import static sample.consumer.failure.ValidationFailure.NONE;
import static sample.consumer.failure.ValidationFailure.NOTHING_TO_VALIDATE;
import static sample.consumer.failure.ValidationFailure.REQUIRED_FIELD_MISSING;
import static sample.consumer.failure.ValidationFailure.REQUIRED_FIELD_MISSING_1;
import static sample.consumer.failure.ValidationFailure.REQUIRED_FIELD_MISSING_2;
import static sample.consumer.failure.ValidationFailure.REQUIRED_LIST_MISSING;
import static sample.consumer.failure.ValidationFailure.getFailureWithParams;

import com.force.swag.id.ID;
import io.vavr.Tuple;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.revcloud.vader.runner.ValidationConfig;
import org.revcloud.vader.specs.Specs;
import sample.consumer.failure.ValidationFailure;
import sample.consumer.failure.ValidationFailureMessage;

/** Contains tests for all DSL common to configs derived from BaseValidationConfig */
class BaseValidationConfigTest {

  // tag::validationConfig-for-flat-bean-demo[]
  @DisplayName("Cases covered - Missing Field, String Field, List Field")
  @Test
  void failFastWithRequiredFieldsMissing() {
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .shouldHaveFieldsOrFailWith(
                Map.of(
                    Bean::getRequiredField1, REQUIRED_FIELD_MISSING_1,
                    Bean::getRequiredField2, REQUIRED_FIELD_MISSING_2,
                    Bean::getRequiredList, REQUIRED_LIST_MISSING))
            .shouldHaveValidSFIdFormatForAllOrFailWith(
                Map.of(
                    Bean::getSfId1, FIELD_INTEGRITY_EXCEPTION,
                    Bean::getSfId2, FIELD_INTEGRITY_EXCEPTION))
            .withValidatorEtr(
                beanEtr -> beanEtr.filterOrElse(Objects::nonNull, ignore -> NOTHING_TO_VALIDATE))
            .prepare();

    final var validatableWithBlankReqField = new Bean(0, "", null, null, List.of("1"));
    final var result1 = validateAndFailFast(validatableWithBlankReqField, validationConfig);
    assertThat(result1).contains(REQUIRED_FIELD_MISSING_2);

    final var validatableWithNullReqField = new Bean(null, "2", null, null, List.of("1"));
    final var result2 = validateAndFailFast(validatableWithNullReqField, validationConfig);
    assertThat(result2).contains(REQUIRED_FIELD_MISSING_1);

    final var validatableWithEmptyReqList = new Bean(1, "2", null, null, emptyList());
    final var result3 = validateAndFailFast(validatableWithEmptyReqList, validationConfig);
    assertThat(result3).contains(REQUIRED_LIST_MISSING);
  }
  // end::validationConfig-for-flat-bean-demo[]

  @Test
  void failFastWithRequiredFieldMissingFailWithFn() {
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .shouldHaveFieldsOrFailWithFn(
                Tuple.of(
                    List.of(
                        Bean::getRequiredField1, Bean::getRequiredField2, Bean::getRequiredList),
                    (missingFieldName, missingFieldValue) ->
                        getFailureWithParams(
                            REQUIRED_FIELD_MISSING,
                            missingFieldName,
                            missingFieldValue + "missing")))
            .prepare();
    final var expectedFieldNames =
        Set.of(Bean.Fields.requiredField1, Bean.Fields.requiredField2, Bean.Fields.requiredList);
    assertThat(validationConfig.getRequiredFieldNames(Bean.class)).isEqualTo(expectedFieldNames);
    final var withRequiredFieldNull = new Bean(1, "", null, null, emptyList());

    final var result = validateAndFailFast(withRequiredFieldNull, validationConfig);
    assertThat(result).isPresent();
    assertThat(result.get().getValidationFailureMessage().getParams())
        .containsExactly(Bean.Fields.requiredField2, "missing");
  }

  @DisplayName("Cases covered - Optional field missing")
  @Test
  void failFastWithRequiredFieldMissingFailWithFn2() {
    final var validationConfig =
        ValidationConfig.<Bean1, ValidationFailure>toValidate()
            .shouldHaveFieldOrFailWithFn(
                Bean1::getStr,
                (fieldName, value) -> {
                  assertThat(fieldName).isEqualTo(Bean1.Fields.str);
                  return REQUIRED_FIELD_MISSING;
                })
            .prepare();
    var bean1 = new Bean1(Optional.empty());
    final var result = validateAndFailFast(bean1, validationConfig);
    assertThat(result).contains(REQUIRED_FIELD_MISSING);
  }

  @Test
  void failFastWithInvalidId() {
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .shouldHaveFieldsOrFailWith(
                Map.of(
                    Bean::getRequiredField1, REQUIRED_FIELD_MISSING,
                    Bean::getRequiredField2, REQUIRED_FIELD_MISSING))
            .shouldHaveValidSFIdFormatForAllOrFailWith(
                Map.of(
                    Bean::getSfId1, FIELD_INTEGRITY_EXCEPTION,
                    Bean::getSfId2, FIELD_INTEGRITY_EXCEPTION))
            .prepare();
    final var validatableWithInvalidSfId =
        new Bean(0, "1", new ID("1ttxx00000000hZAAQ"), new ID("invalidSfId"), emptyList());
    final var result = validateAndFailFast(validatableWithInvalidSfId, validationConfig);
    assertThat(result).contains(FIELD_INTEGRITY_EXCEPTION);
  }

  @Test
  void failFastWithInvalidIdFailWithFn() {
    final var validationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .shouldHaveValidSFIdFormatForAllOrFailWithFn(
                Tuple.of(
                    List.of(Bean::getSfId1, Bean::getSfId2),
                    (name, value) ->
                        getFailureWithParams(
                            ValidationFailureMessage.MSG_WITH_PARAMS, name, value)))
            .prepare();
    final var expectedFieldNames = Set.of(Bean.Fields.sfId1, Bean.Fields.sfId2);
    assertThat(validationConfig.getRequiredFieldNamesForSFIdFormat(Bean.class))
        .isEqualTo(expectedFieldNames);
    final var invalidSfId = new ID("invalidSfId");
    final var validatableWithInvalidSfId =
        new Bean(null, null, new ID("1ttxx00000000hZAAQ"), invalidSfId, emptyList());
    final var result = validateAndFailFast(validatableWithInvalidSfId, validationConfig);
    assertThat(result).isPresent();
    assertThat(result.get().getValidationFailureMessage().getParams())
        .containsExactly(Bean.Fields.sfId2, invalidSfId);
  }

  @Test
  void getSpecWithNameWithDuplicateNames() {
    val duplicateSpecName = "DuplicateSpecName";
    final var specsForConfig =
        (Specs<BeanWithIdFields, ValidationFailure>)
            spec ->
                List.of(
                    spec._1()
                        .nameForTest(duplicateSpecName)
                        .given(BeanWithIdFields::getRequiredField),
                    spec._1().nameForTest(duplicateSpecName).given(BeanWithIdFields::getContactId));
    final var validationConfig =
        ValidationConfig.<BeanWithIdFields, ValidationFailure>toValidate()
            .specify(specsForConfig)
            .prepare();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> validationConfig.getPredicateOfSpecForTest(duplicateSpecName));
  }

  @Test
  void getFieldNames() {
    final var validationConfig =
        ValidationConfig.<BeanWithIdFields, ValidationFailure>toValidate()
            .shouldHaveFieldOrFailWith(BeanWithIdFields::getRequiredField, NONE)
            .shouldHaveValidSFIdFormatOrFailWith(BeanWithIdFields::getAccountId, NONE)
            .absentOrHaveValidSFIdFormatOrFailWith(BeanWithIdFields::getContactId, NONE)
            .prepare();
    assertThat(validationConfig.getRequiredFieldNames(BeanWithIdFields.class))
        .contains(BeanWithIdFields.Fields.requiredField);
    assertThat(validationConfig.getRequiredFieldNamesForSFIdFormat(BeanWithIdFields.class))
        .contains(BeanWithIdFields.Fields.accountId);
    assertThat(validationConfig.getNonRequiredFieldNamesForSFIdFormat(BeanWithIdFields.class))
        .contains(BeanWithIdFields.Fields.contactId);
  }

  // tag::validationConfig-for-nested-bean-demo[]
  @Test
  void nestedBeanValidationWithInvalidMember() {
    final var memberValidationConfig =
        ValidationConfig.<Bean, ValidationFailure>toValidate()
            .shouldHaveValidSFIdFormatForAllOrFailWithFn(
                Tuple.of(
                    List.of(Bean::getSfId1, Bean::getSfId2),
                    (name, value) ->
                        getFailureWithParams(
                            ValidationFailureMessage.MSG_WITH_PARAMS, name, value)))
            .prepare();
    final var containerValidationConfig =
        ValidationConfig.<ContainerBean, ValidationFailure>toValidate()
            .shouldHaveFieldOrFailWithFn(
                ContainerBean::getRequiredField,
                (name, value) ->
                    getFailureWithParams(ValidationFailureMessage.MSG_WITH_PARAMS, name, value))
            .prepare();

    final var invalidSfId = new ID("invalidSfId");
    final var memberWithInvalidSfId =
        new Bean(null, null, new ID("1ttxx00000000hZAAQ"), invalidSfId, emptyList());
    final var validContainer = new ContainerBean("requiredField", memberWithInvalidSfId);
    final var result =
        validateAndFailFast(validContainer, containerValidationConfig)
            .or(() -> validateAndFailFast(memberWithInvalidSfId, memberValidationConfig));

    assertThat(result).isPresent();
    assertThat(result.get().getValidationFailureMessage().getParams())
        .containsExactly(Bean.Fields.sfId2, invalidSfId);
  }
  // end::validationConfig-for-nested-bean-demo[]

  @Data
  @AllArgsConstructor
  @FieldNameConstants
  public static class BeanWithIdStrFields {
    String requiredField;
    String accountId;
    String contactId;
  }

  @Data
  @AllArgsConstructor
  @FieldNameConstants
  public static class BeanWithMixIdFields {
    String requiredField;
    ID accountId;
    String contactId;
  }

  @Data
  @FieldNameConstants
  @AllArgsConstructor
  // tag::nested-bean[]
  // tag::flat-bean[]
  public static class Bean {
    private final Integer requiredField1;
    private final String requiredField2;
    private final ID sfId1;
    private final ID sfId2;
    private final List<String> requiredList;
  }
  // end::flat-bean[]
  // end::nested-bean[]

  @Value
  // tag::nested-bean[]
  public static class ContainerBean {
    String requiredField;
    Bean bean;
  }
  // end::nested-bean[]

  @Data
  @FieldNameConstants
  @AllArgsConstructor
  public static class BeanWithIdFields {
    String requiredField;
    ID accountId;
    ID contactId;
  }

  @Data
  @FieldNameConstants
  @AllArgsConstructor
  public static class Bean1 {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<String> str;
  }
}
