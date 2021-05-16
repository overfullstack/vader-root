package org.revcloud.vader.runner

import consumer.bean.Parent
import consumer.failure.ValidationFailure
import io.kotest.core.spec.style.FunSpec

class ConfigToValidatorsTest : FunSpec({

    test("toValidators1 with null") {
        println(toValidators2<Parent, ValidationFailure, Boolean>(null) { false })
    }
})
