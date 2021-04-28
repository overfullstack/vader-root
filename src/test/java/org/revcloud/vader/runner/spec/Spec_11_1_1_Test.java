package org.revcloud.vader.runner.spec;

import consumer.failure.ValidationFailure;
import lombok.Value;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.revcloud.vader.runner.ValidationConfig;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.revcloud.vader.DateMatchers.isBefore;

class Spec_11_1_1_Test {
    @Test
    void testDates() {
        val specName = "CompareDates";
        val validationConfig = ValidationConfig.<Bean, ValidationFailure>toValidate().withSpec(spec ->
                spec._11_1_1().nameForTest(specName)
                        .when(Bean::isCompareDates)
                        .is(true)
                        .thenField1(Bean::getDate1)
                        .thenField2(Bean::getDate2)
                        .shouldRelateWithFn(isBefore())).prepare();

        val validBean = new Bean(true, new GregorianCalendar(2021, Calendar.APRIL, 27).getTime(),
                new GregorianCalendar(2021, Calendar.APRIL, 28).getTime());
        assertTrue(validationConfig.getSpecWithName(specName).map(spec -> spec.test(validBean)).orElse(false));

        val invalidBean = new Bean(true, new GregorianCalendar(2021, Calendar.APRIL, 29).getTime(),
                new GregorianCalendar(2021, Calendar.APRIL, 28).getTime());
        assertFalse(validationConfig.getSpecWithName(specName).map(spec -> spec.test(invalidBean)).orElse(true));
    }

    @Test
    void testDatesWithNulls() {
        val specName = "CompareDates";
        val validationConfig = ValidationConfig.<Bean, ValidationFailure>toValidate().withSpec(spec ->
                spec._11_1_1().nameForTest(specName)
                        .when(Bean::isCompareDates)
                        .is(true)
                        .thenField1(Bean::getDate1)
                        .thenField2(Bean::getDate2)
                        .shouldRelateWithFn(isBefore())).prepare();
        val invalidBean1 = new Bean(true, null, new GregorianCalendar(2021, Calendar.APRIL, 29).getTime());
        assertFalse(validationConfig.getSpecWithName(specName).map(spec -> spec.test(invalidBean1)).orElse(true));

        val invalidBean2 = new Bean(true, new GregorianCalendar(2021, Calendar.APRIL, 29).getTime(), null);
        assertFalse(validationConfig.getSpecWithName(specName).map(spec -> spec.test(invalidBean2)).orElse(true));
    }

    @Value
    private static class Bean {
        boolean compareDates;
        Date date1;
        Date date2;
    }
}