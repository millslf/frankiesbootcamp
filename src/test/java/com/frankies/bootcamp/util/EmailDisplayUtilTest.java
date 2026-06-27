package com.frankies.bootcamp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailDisplayUtilTest {
    @Test
    void masksTypicalEmailAddress() {
        assertEquals("f***s@gmail.com", EmailDisplayUtil.maskEmail("francois@gmail.com"));
    }

    @Test
    void leavesNonEmailValuesUntouched() {
        assertEquals("not-an-email", EmailDisplayUtil.maskEmail("not-an-email"));
    }
}
