/*
 * Copyright 2020 salesforce.com, inc.
 * All Rights Reserved
 * Company Confidential
 */

package org.revcloud.vader.dsl.lift;

import io.vavr.Function1;
import io.vavr.collection.List;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.revcloud.vader.types.validators.SimpleValidator;

/**
 * DSL to lift simple member validations to container type.
 * 
 *  @author gakshintala
 *  @since 228
 */
@UtilityClass
public class AggregationLiftSimpleDsl {

    /**
     * Lifts a list of simple member validations to container type.
     * @param childValidations  List of member validations
     * @param toChildMapper     Mapper function to extract member from container
     * @param invalidParent     Failure to return if container is null
     * @param invalidChild      Failure to return if member is null
     * @param <ContainerT>
     * @param <MemberT>
     * @param <FailureT>
     * @return                  List of container type validations
     */
    public static <ContainerT, MemberT, FailureT> List<SimpleValidator<ContainerT, FailureT>> liftAllToParentValidationType(
            List<SimpleValidator<MemberT, FailureT>> childValidations,
            Function1<ContainerT, MemberT> toChildMapper, FailureT invalidParent, FailureT invalidChild) {
        return childValidations.map(childValidation ->
                liftToParentValidationType(childValidation, toChildMapper, invalidParent, invalidChild));
    }

    /**
     * Lifts a list of simple member validations to container type.
     * IMP: This doesn't do a null check on member, so the member validation is supposed to take that responsibility.
     * @param childValidations  List of member validations
     * @param toChildMapper     Mapper function to extract member from container
     * @param invalidParent     Failure to return if container is null
     * @param <ContainerT>
     * @param <MemberT>
     * @param <FailureT>
     * @return                  List of container type validations
     */
    public static <ContainerT, MemberT, FailureT> List<SimpleValidator<ContainerT, FailureT>> liftAllToParentValidationType(
            List<SimpleValidator<MemberT, FailureT>> childValidations,
            Function1<ContainerT, MemberT> toChildMapper, FailureT invalidParent) {
        return childValidations.map(childValidation ->
                liftToParentValidationType(childValidation, toChildMapper, invalidParent));
    }

    /**
     * Lifts a simple member validation to container type.
     * @param childValidation
     * @param toChildMapper Mapper function to extract member from container
     * @param invalidParent Failure to return if container is null
     * @param invalidChild  Failure to return if member is null
     * @param <ContainerT>
     * @param <MemberT>
     * @param <FailureT>
     * @return  container type validation
     */
    public static <ContainerT, MemberT, FailureT> SimpleValidator<ContainerT, FailureT> liftToParentValidationType(
            SimpleValidator<MemberT, FailureT> childValidation,
            Function1<ContainerT, MemberT> toChildMapper,
            FailureT invalidParent, FailureT invalidChild) {
        return validatedParent -> {
            if (validatedParent == null) {
                return invalidParent;
            } else {
                val member = toChildMapper.apply(validatedParent);
                return member == null ? invalidChild : childValidation.apply(member);
            }
        };
    }

    /**
     * Lifts a member validation to container type.
     * IMP: This doesn't do a null check on member. If the Member is null, the validation throws a NPE while executing.
     *      So the member validation is supposed to take that responsibility to check for null.
     *      This is specific to validations which want to check other params, based on member being null.  
     * @param childValidation
     * @param toChildMapper Mapper function to extract member from container
     * @param invalidParent Failure to return if container is null
     * @param <ContainerT>
     * @param <MemberT>
     * @param <FailureT>
     * @return  container type validation
     */
    public static <ContainerT, MemberT, FailureT> SimpleValidator<ContainerT, FailureT> liftToParentValidationType(
            SimpleValidator<MemberT, FailureT> childValidation,
            Function1<ContainerT, MemberT> toChildMapper,
            FailureT invalidParent) {
        return validatedParent -> (validatedParent == null) ? invalidParent : childValidation.apply(toChildMapper.apply(validatedParent));
    }
}
