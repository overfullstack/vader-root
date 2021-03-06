package org.revcloud.vader.types.failures

import io.vavr.Tuple2
import io.vavr.control.Either

class FFABatchOfBatchFailureWithPair<ContainerPairT, MemberPairT, FailureT>(val failure: Either<Tuple2<ContainerPairT?, FailureT?>, Tuple2<MemberPairT?, FailureT?>>) :
  Either<Tuple2<ContainerPairT?, FailureT?>, Tuple2<MemberPairT?, FailureT?>> by failure {
  val containerFailure: Tuple2<ContainerPairT?, FailureT?>?
    get() = if (failure.isLeft) failure.left else null

  val batchMemberFailure: Tuple2<MemberPairT?, FailureT?>?
    get() = if (failure.isRight) failure.get() else null
}
