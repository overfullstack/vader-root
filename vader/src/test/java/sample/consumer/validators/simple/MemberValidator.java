/*
 * Copyright 2019 salesforce.com, inc.
 * All Rights Reserved
 * Company Confidential
 */

package sample.consumer.validators.simple;

import static sample.consumer.failure.ValidationFailureMessage.FIELD_NULL_OR_EMPTY;

import org.revcloud.vader.types.Validator;
import sample.consumer.bean.Member;
import sample.consumer.failure.ValidationFailure;

public class MemberValidator {

  public static final Validator<Member, ValidationFailure> validator1 =
      member -> {
        if (member == null) {
          return new ValidationFailure(FIELD_NULL_OR_EMPTY);
        } else {
          return ValidationFailure.NONE;
        }
      };

  static final Validator<Member, ValidationFailure> validator2 =
      member -> {
        if (member == null) {
          return new ValidationFailure(FIELD_NULL_OR_EMPTY);
        } else {
          return ValidationFailure.NONE;
        }
      };
}
