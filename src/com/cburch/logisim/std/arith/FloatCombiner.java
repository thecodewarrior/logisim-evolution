package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.util.StringGetter;

public class FloatCombiner extends InstanceFactory {
    static final int PER_DELAY = 1;
    private static final int IN_SIGN = 0;
    private static final int IN_EXPONENT = 1;
    private static final int IN_SIGNIFICAND = 2;
    private static final int OUT = 3;

    private static final BitWidth EXPONENT_WIDTH = BitWidth.create(8);
    private static final BitWidth SIGNIFICAND_WIDTH = BitWidth.create(23);

    public FloatCombiner() {
        super("FloatCombiner", Strings.getter("floatCombinerComponent"));
        setAttributes(new Attribute[] {}, new Object[] {});
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));

        Port[] ps = new Port[4];
        ps[0] = new Port(-20, -20, Port.INPUT, BitWidth.ONE);
        ps[1] = new Port(-40, -10, Port.INPUT, EXPONENT_WIDTH);
        ps[2] = new Port(-40,  10, Port.INPUT, SIGNIFICAND_WIDTH);
        ps[3] = new Port(0, 0, Port.OUTPUT, BitWidth.FLOAT);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.drawBounds();
        painter.drawPort(IN_SIGN, "-", Direction.SOUTH);
        painter.drawPort(IN_EXPONENT, "exp", Direction.EAST);
        painter.drawPort(IN_SIGNIFICAND, "sgn", Direction.EAST);
        painter.drawPort(OUT);
    }

    @Override
    public void propagate(InstanceState state) {
        // compute outputs
        Value sign = state.getPortValue(IN_SIGN);
        Value exponent = state.getPortValue(IN_EXPONENT);
        Value significand = state.getPortValue(IN_SIGNIFICAND);

        Value out;
        if(sign.isFullyDefined() && exponent.isFullyDefined() && significand.isFullyDefined()) {
            long signL = sign.toIntValue() > 0 ? 0x80000000L : 0x00000000;
            long exponentL = (exponent.toIntValue() & 0x000000FFL) << 23;
            long significandL = (exponent.toIntValue() & 0x007fffffL);
            out = Value.createKnown(BitWidth.FLOAT, (int)(signL | exponentL | significandL));
        } else if(sign.isErrorValue() || exponent.isErrorValue() || significand.isFullyDefined()) {
            out = Value.createError(BitWidth.FLOAT);
        } else {
            out = Value.createUnknown(BitWidth.FLOAT);
        }

        // propagate them
        int delay = 1;
        state.setPort(OUT, out, delay);
    }
}
