/*
 * Copyright 2019 salesforce.com, inc.
 * All Rights Reserved
 * Company Confidential
 */

package sample.consumer.validators.etr;

import org.revcloud.vader.types.ValidatorEtr;
import sample.consumer.bean.Container;
import sample.consumer.failure.ValidationFailure;
import sample.consumer.failure.ValidationFailureMessage;

public class ContainerValidatorEtr {

  public static final ValidatorEtr<Container, ValidationFailure> validatorEtr1 =
      containerEtr ->
          containerEtr.filterOrElse(
              container -> container.getMember() != null,
              ignore -> new ValidationFailure(ValidationFailureMessage.FIELD_NULL_OR_EMPTY));

  public static final ValidatorEtr<Container, ValidationFailure> validatorEtr2 =
      containerEtr ->
          containerEtr.filterOrElse(
              container -> container.getMember() != null,
              ignore -> new ValidationFailure(ValidationFailureMessage.FIELD_NULL_OR_EMPTY));
}
