package org.revcloud.vader.dsl.runner;

import com.force.swag.id.ID;
import com.force.swag.id.IdTraits;
import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.revcloud.vader.config.BaseValidationConfig;
import org.revcloud.vader.config.BatchValidationConfig;
import org.revcloud.vader.config.HeaderValidationConfig;
import org.revcloud.vader.config.ValidationConfig;
import org.revcloud.vader.types.validators.SimpleValidator;
import org.revcloud.vader.types.validators.Validator;

import java.util.Objects;
import java.util.function.Predicate;

import static io.vavr.CheckedFunction1.liftTry;
import static io.vavr.Function1.identity;

@Slf4j
@UtilityClass
class Utils {

    static <FailureT, ValidatableT> Iterator<Either<FailureT, ValidatableT>> fireValidators(
            Either<FailureT, ValidatableT> toBeValidatedRight, // TODO: 28/03/21 toBeValidated vs Validatable naming consistency
            Iterator<Validator<ValidatableT, FailureT>> validators,
            Function1<Throwable, FailureT> throwableMapper) {
        return validators
                .map(currentValidator -> fireValidator(currentValidator, toBeValidatedRight, throwableMapper));
    }

    static <FailureT, ValidatableT> Either<FailureT, ValidatableT> fireValidator(
            Validator<ValidatableT, FailureT> validator,
            Either<FailureT, ValidatableT> toBeValidatedRight,
            Function1<Throwable, FailureT> throwableMapper) {
        return liftTry(validator).apply(toBeValidatedRight)
                .fold(throwable -> Either.left(throwableMapper.apply(throwable)), Function1.identity())
                .flatMap(ignore -> toBeValidatedRight); // Put the original Validatable in the right state
    }

    static <FailureT> FailureT validateSize(java.util.List<?> validatables,
                                            FailureT none,
                                            HeaderValidationConfig<?, FailureT> validationConfig) {
        if (validatables.size() < validationConfig.getMinBatchSize()._1) {
            return validationConfig.getMinBatchSize()._2;
        } else if (validatables.size() > validationConfig.getMaxBatchSize()._1) {
            return validationConfig.getMaxBatchSize()._2;
        }
        return none;
    }

    static <FailureT, ValidatableT> Iterator<FailureT> applySimpleValidators(
            ValidatableT toBeValidated,
            Iterator<SimpleValidator<ValidatableT, FailureT>> validators,
            Function1<Throwable, FailureT> throwableMapper) {
        return validators.map(validator -> fireSimpleValidator(validator, toBeValidated, throwableMapper));
    }

    private static <FailureT, ValidatableT> FailureT fireSimpleValidator(
            SimpleValidator<ValidatableT, FailureT> validator,
            ValidatableT validatable,
            Function1<Throwable, FailureT> throwableMapper) {
        return Try.of(() -> validator.apply(validatable)).fold(throwableMapper, identity());
    }

    static <ValidatableT, FailureT> Iterator<Validator<ValidatableT, FailureT>> toValidators(
            BaseValidationConfig<ValidatableT, FailureT> validationConfig) {
        Iterator<Validator<ValidatableT, FailureT>> mandatoryFieldValidators = validationConfig.getMandatoryFieldMappers().iterator()
                .map(tuple2 -> validatableRight -> validatableRight.map(tuple2._1).filterOrElse(isPresent, ignore -> tuple2._2));
        Iterator<Validator<ValidatableT, FailureT>> mandatorySfIdValidators = validationConfig.getMandatorySfIdFieldMappers().iterator()
                .map(tuple2 -> validatableRight -> validatableRight.map(tuple2._1).map(ID::toString)
                        .filterOrElse(IdTraits::isValidId, ignore -> tuple2._2));
        Iterator<Validator<ValidatableT, FailureT>> nonMandatorySfIdValidators = validationConfig.getNonMandatorySfIdFieldMappers().iterator()
                .map(tuple2 -> validatableRight -> validatableRight.map(tuple2._1).map(ID::toString)
                        .filter(Objects::nonNull) // Ignore if null
                        .fold(() -> validatableRight, id -> id.filterOrElse(IdTraits::isValidId, ignore -> tuple2._2)));
        return mandatoryFieldValidators.concat(mandatorySfIdValidators).concat(nonMandatorySfIdValidators);
    }

    private static final Predicate<Object> isPresent = fieldValue -> {
        if (fieldValue != null) {
            if (fieldValue instanceof String) {
                return !((String) fieldValue).isBlank();
            }
            return true;
        }
        return false;
    };

    static <ValidatableT, FailureT> Seq<Either<FailureT, ValidatableT>> filterInvalidatablesAndDuplicates(
            List<ValidatableT> validatables,
            FailureT invalidValidatable,
            BatchValidationConfig<ValidatableT, FailureT> batchValidationConfig) {
        if (validatables.isEmpty()) {
            return List.empty();
        } else if (validatables.size() == 1) {
            val validatable = validatables.get(0);
            return validatable == null ? List.of(Either.left(invalidValidatable)) : List.of(Either.right(validatables.get(0)));
        }
        final var filterDuplicatesConfig = batchValidationConfig.getFilterDuplicates();
        val keyMapperForDuplicates = filterDuplicatesConfig == null
                ? Function1.<ValidatableT>identity()
                : filterDuplicatesConfig._2;
        val groups = validatables.zipWithIndex()
                .groupBy(tuple2 -> tuple2._1 == null ? null : keyMapperForDuplicates.apply(tuple2._1));

        groups.forEach(group -> log.info(group.toString()));
        Seq<Tuple2<Either<FailureT, ValidatableT>, Integer>> invalidValidatables = groups.get(null)
                .map(nullValidatables -> invalidate(nullValidatables, invalidValidatable))
                .getOrElse(List.empty());

        val partition = groups.remove(null).values().partition(group -> group.size() == 1);
        val failureForDuplicate = batchValidationConfig.getFilterDuplicates()._1;
        Seq<Tuple2<Either<FailureT, ValidatableT>, Integer>> duplicates =
                partition._2.flatMap(identity()).map(duplicate -> Tuple.of(Either.left(failureForDuplicate), duplicate._2));
        Seq<Tuple2<Either<FailureT, ValidatableT>, Integer>> nonDuplicates =
                partition._1.flatMap(identity()).map(tuple2 -> tuple2.map1(Either::right));

        return duplicates.appendAll(nonDuplicates).appendAll(invalidValidatables).sortBy(Tuple2::_2).map(Tuple2::_1);
    }

    private static <FailureT, ValidatableT> Seq<Tuple2<Either<FailureT, ValidatableT>, Integer>> invalidate(
            Seq<Tuple2<ValidatableT, Integer>> nullValidatables, FailureT invalidValidatable) {
        return nullValidatables.map(nullValidatable -> nullValidatable.map1(ignore -> Either.left(invalidValidatable)));
    }

    static <ValidatableT, FailureT> Iterator<SimpleValidator<ValidatableT, FailureT>> toSimpleValidators(
            ValidationConfig<ValidatableT, FailureT> validationConfig, FailureT none) {
        Iterator<SimpleValidator<ValidatableT, FailureT>> mandatoryFieldValidators = validationConfig.getMandatoryFieldMappers().iterator()
                .map(tuple2 -> validatable -> isPresent.test(tuple2._1.apply(validatable)) ? none : tuple2._2);
        Iterator<SimpleValidator<ValidatableT, FailureT>> sfIdValidators = validationConfig.getMandatorySfIdFieldMappers().iterator()
                .map(tuple2 -> validatable -> IdTraits.isValidId(tuple2._1.apply(validatable).toString()) ? none : tuple2._2);
        return mandatoryFieldValidators.concat(sfIdValidators);
    }
}
