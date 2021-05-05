package org.revcloud.vader.runner;

import com.force.swag.id.ID;
import consumer.failure.ValidationFailure;
import consumer.failure.ValidationFailureMessage;
import io.vavr.Tuple;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static consumer.failure.ValidationFailure.FIELD_INTEGRITY_EXCEPTION;
import static consumer.failure.ValidationFailure.REQUIRED_FIELD_MISSING;
import static consumer.failure.ValidationFailure.UNKNOWN_EXCEPTION;
import static consumer.failure.ValidationFailure.getFailureWithParams;
import static org.assertj.core.api.Assertions.assertThat;

class RunnerWithConfigTest {

    @Test
    void failFastWithRequiredFieldsMissingForSimpleValidators() {
        val validationConfig = ValidationConfig.<Bean, ValidationFailure>toValidate()
                .shouldHaveFieldsOrFailWith(Map.of(
                        Bean::getRequiredField1, REQUIRED_FIELD_MISSING,
                        Bean::getRequiredField2, REQUIRED_FIELD_MISSING))
                .shouldHaveValidSFIdFormatOrFailWith(Map.of(
                        Bean::getSfId1, FIELD_INTEGRITY_EXCEPTION,
                        Bean::getSfId2, FIELD_INTEGRITY_EXCEPTION)).prepare();

        val validatableWithBlankReqField = new Bean(0, "", null, null);
        val result1 = Runner.validateAndFailFast(
                validatableWithBlankReqField,
                throwable -> UNKNOWN_EXCEPTION,
                validationConfig);
        assertThat(result1).contains(REQUIRED_FIELD_MISSING);

        val validatableWithNullReqField = new Bean(0, null, null, null);
        val result2 = Runner.validateAndFailFast(
                validatableWithNullReqField,
                throwable -> UNKNOWN_EXCEPTION,
                validationConfig);
        assertThat(result2).contains(REQUIRED_FIELD_MISSING);
    }

    @Test
    void failFastWithRequiredFieldsWithNameMissingForSimpleValidators() {
        val validationConfig = ValidationConfig.<Bean, ValidationFailure>toValidate()
                .shouldHaveFieldsOrFailWithFn(Tuple.of(List.of(
                        Bean::getRequiredField1,
                        Bean::getRequiredField2),
                        (name, value) -> getFailureWithParams(ValidationFailureMessage.MSG_WITH_PARAMS, name, value)))
                .prepare();
        val expectedFieldNames = Set.of(Bean.Fields.requiredField1, Bean.Fields.requiredField2);
        assertThat(validationConfig.getRequiredFieldNames(Bean.class)).isEqualTo(expectedFieldNames);
        val withRequiredFieldNull = new Bean(1, "", null, null);
        val result = Runner.validateAndFailFast(
                withRequiredFieldNull,
                throwable -> UNKNOWN_EXCEPTION,
                validationConfig);
        assertThat(result).isPresent();
        assertThat(result.get().getValidationFailureMessage().getParams()).containsExactly(Bean.Fields.requiredField2, "");
    }

    @Test
    void failFastWithInvalidIdForSimpleValidators() {
        val validationConfig = ValidationConfig.<Bean, ValidationFailure>toValidate()
                .shouldHaveFieldsOrFailWith(Map.of(
                        Bean::getRequiredField1, REQUIRED_FIELD_MISSING,
                        Bean::getRequiredField2, REQUIRED_FIELD_MISSING))
                .shouldHaveValidSFIdFormatOrFailWith(Map.of(
                        Bean::getSfId1, FIELD_INTEGRITY_EXCEPTION,
                        Bean::getSfId2, FIELD_INTEGRITY_EXCEPTION)).prepare();

        val validatableWithInvalidSfId = new Bean(0, "1", new ID("1ttxx00000000hZAAQ"), new ID("invalidSfId"));
        val result1 = Runner.validateAndFailFast(
                validatableWithInvalidSfId,
                ValidationFailure::getValidationFailureForException,
                validationConfig);
        assertThat(result1).contains(FIELD_INTEGRITY_EXCEPTION);
    }

    @Test
    void failFastWithInvalidIdWithNameForSimpleValidators() {
        val validationConfig = ValidationConfig.<Bean, ValidationFailure>toValidate()
                .shouldHaveValidSFIdFormatOrFailWithFn(Tuple.of(List.of(Bean::getSfId1, Bean::getSfId2),
                        (name, value) -> getFailureWithParams(ValidationFailureMessage.MSG_WITH_PARAMS, name, value)))
                .prepare();
        val expectedFieldNames = Set.of(Bean.Fields.sfId1, Bean.Fields.sfId2);
        assertThat(validationConfig.getRequiredFieldNamesForSFIdFormat(Bean.class)).isEqualTo(expectedFieldNames);
        val invalidSfId = new ID("invalidSfId");
        val validatableWithInvalidSfId = new Bean(null, null, new ID("1ttxx00000000hZAAQ"), invalidSfId);
        val result = Runner.validateAndFailFast(
                validatableWithInvalidSfId,
                ValidationFailure::getValidationFailureForException,
                validationConfig);
        assertThat(result).isPresent();
        assertThat(result.get().getValidationFailureMessage().getParams()).containsExactly(Bean.Fields.sfId2, invalidSfId);
    }

    @Data
    @FieldNameConstants
    public static class Bean {
        private final Integer requiredField1;
        private final String requiredField2;
        private final ID sfId1;
        private final ID sfId2;
    }
}
