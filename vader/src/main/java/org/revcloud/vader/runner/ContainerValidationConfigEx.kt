@file:JvmName("ContainerValidationConfigEx")

package org.revcloud.vader.runner

import de.cronn.reflection.util.PropertyUtils
import org.revcloud.vader.types.ValidatorEtr

internal fun <ContainerValidatableT, FailureT> BaseContainerValidationConfig<ContainerValidatableT?, FailureT?>.getContainerValidatorsEx(): List<ValidatorEtr<ContainerValidatableT?, FailureT?>> =
  fromValidators1(withContainerValidators) + fromValidators2(withContainerValidator) + withContainerValidatorEtrs

internal fun <ContainerValidatableT, FailureT> ContainerValidationConfig<ContainerValidatableT?, FailureT?>.getFieldNamesForBatchEx(
  validatableClazz: Class<ContainerValidatableT>
): Set<String> =
  withBatchMembers.map { PropertyUtils.getPropertyName(validatableClazz, it) }.toSet()

internal fun <ContainerValidatableT, FailureT> ContainerValidationConfigWith2Levels<ContainerValidatableT?, *, FailureT?>.getFieldNamesForBatchEx(
  validatableClazz: Class<ContainerValidatableT>
): Set<String> =
  withBatchMembers.map { PropertyUtils.getPropertyName(validatableClazz, it) }.toSet()

internal fun <NestedContainerValidatableT, FailureT> ContainerValidationConfigWith2Levels<*, NestedContainerValidatableT?, FailureT?>.getFieldNamesForBatchLevel1Ex(
  validatableClazz: Class<NestedContainerValidatableT>
): Set<String> =
  withScopeOf1LevelDeep.withBatchMembers.map { PropertyUtils.getPropertyName(validatableClazz, it) }.toSet()
