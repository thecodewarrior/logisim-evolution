package com.cburch.logisim.std.arith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface ALUOperation {
    long run(long left, long right, int width, boolean signed);

    static ALUOperation[] operations = new ALUOperation[] {
            (long left, long right, int width, boolean signed) ->                   //
                    left + right,                                                   // 00 (0x00): +
            (long left, long right, int width, boolean signed) ->                   //
                    left - right,                                                   // 01 (0x01): -
            (long left, long right, int width, boolean signed) ->                   //
                    left * right,                                                   // 02 (0x02): *
            (long left, long right, int width, boolean signed) ->                   //
                    left / right,                                                   // 03 (0x03): /
            (long left, long right, int width, boolean signed) ->                   //
                    left % right,                                                   // 04 (0x04): %
            (long left, long right, int width, boolean signed) ->                   //
                    -left,                                                          // 05 (0x05): -x
            (long left, long right, int width, boolean signed) ->                   //
                    left << right,                                                  // 06 (0x06): <<
            (long left, long right, int width, boolean signed) ->                   //
                    signed ? left >> right : left >>> right,                        // 07 (0x07): >>
            (long left, long right, int width, boolean signed) ->                   //
                    (left << right) | ((left >> (width-right)) & (1 << right - 1)), // 08 (0x08): << (rot)
            (long left, long right, int width, boolean signed) ->                   //
                    (left >>> right) | (left << (width - right)),                   // 09 (0x09): >> (rot)
            (long left, long right, int width, boolean signed) ->                   //
                    ~left,                                                          // 10 (0x0a): ~
            (long left, long right, int width, boolean signed) ->                   //
                    left & right,                                                   // 11 (0x0b): &
            (long left, long right, int width, boolean signed) ->                   //
                    left | right,                                                   // 12 (0x0c): |
            (long left, long right, int width, boolean signed) ->                   //
                    left ^ right,                                                   // 13 (0x0d): ^
            (long left, long right, int width, boolean signed) ->                   //
                    i(!b(left)),                                                    // 14 (0x0e): !
            (long left, long right, int width, boolean signed) ->                   //
                    i(b(left) && b(right)),                                         // 15 (0x0f): &&
            (long left, long right, int width, boolean signed) ->                   //
                    i(b(left) || b(right)),                                         // 16 (0x10): ||
            (long left, long right, int width, boolean signed) ->                   //
                    i(b(left) ^ b(right)),                                          // 17 (0x11): ^
            (long left, long right, int width, boolean signed) ->                   //
                    i(left == right),                                               // 18 (0x12): ==
            (long left, long right, int width, boolean signed) ->                   //
                    i(left != right),                                               // 19 (0x13): !=
            (long left, long right, int width, boolean signed) ->                   //
                    i(left > right),                                                // 20 (0x14): >
            (long left, long right, int width, boolean signed) ->                   //
                    i(left < right),                                                // 21 (0x15): <
            (long left, long right, int width, boolean signed) ->                   //
                    i(left >= right),                                               // 22 (0x16): >=
            (long left, long right, int width, boolean signed) ->                   //
                    i(left <= right),                                               // 23 (0x17): <=
    };

    static Set<Integer> signed = new HashSet<>(Arrays.asList(2,3,4,7,20,21,22,23));

    static boolean b(long value) {
        return value > 0;
    }
    static long i(boolean value) {
        return value ? 1 : 0;
    }
}
