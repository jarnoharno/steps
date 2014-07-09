package com.hiit.steps;

import junit.framework.TestCase;

public class ConversionTest extends TestCase {

    // intsToLong

    public void testIntsToLongBothPositive() {
        int a = 0xff;
        int b = 0xff;
        long result = Conversion.intsToLong(a, b);
        long expect = 0x000000ff000000ffl;
        assertEquals(result, expect);
    }

    public void testIntsToLongFirstPositive() {
        int a = 0xff;
        int b = 0xff00ff00;
        long result = Conversion.intsToLong(a, b);
        long expect = 0x000000ffff00ff00l;
        assertEquals(result, expect);
    }

    public void testIntsToLongSecondPositive() {
        int a = 0xff00ff00;
        int b = 0xff;
        long result = Conversion.intsToLong(a, b);
        long expect = 0xff00ff00000000ffl;
        assertEquals(result, expect);
    }

    public void testIntsToLongBothNegative() {
        int a = 0xff00ff00;
        int b = 0xaf00af00;
        long result = Conversion.intsToLong(a, b);
        long expect = 0xff00ff00af00af00l;
        assertEquals(result, expect);
    }

    // longToIntA

    public void testLongToIntAPositive() {
        long l = 0x124300ba0354af43l;
        long result = Conversion.longToIntA(l);
        long expect = 0x124300ba;
        assertEquals(result, expect);
    }

    public void testLongToIntANegative() {
        long l = 0xa24300ba0354af43l;
        long result = Conversion.longToIntA(l);
        long expect = 0xa24300ba;
        assertEquals(result, expect);
    }

    // longToIntB

    public void testLongToIntBPositive() {
        long l = 0x124300ba0354af43l;
        long result = Conversion.longToIntB(l);
        long expect = 0x0354af43;
        assertEquals(result, expect);
    }

    public void testLongToIntBNegative() {
        long l = 0xa24300bab354af43l;
        long result = Conversion.longToIntB(l);
        long expect = 0xb354af43;
        assertEquals(result, expect);
    }

    public void testLongToIntBLongNegative() {
        long l = 0xa24300ba2354af43l;
        long result = Conversion.longToIntB(l);
        long expect = 0x2354af43;
        assertEquals(result, expect);
    }

    public void testLongToIntBIntNegative() {
        long l = 0x624300bac354af43l;
        long result = Conversion.longToIntB(l);
        long expect = 0xc354af43;
        assertEquals(result, expect);
    }

}
