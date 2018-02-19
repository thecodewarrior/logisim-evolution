package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.util.StringGetter;

public class FloatNegator extends InstanceFactory {
    static final int PER_DELAY = 1;
    private static final int IN = 0;
    private static final int OUT = 1;

    public FloatNegator() {
        super("FloatNegator", Strings.getter("floatNegatorComponent"));
        setAttributes(new Attribute[] {}, new Object[] {});
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));

        Port[] ps = new Port[2];
        ps[IN] = new Port(-40, 0, Port.INPUT, BitWidth.FLOAT);
        ps[OUT] = new Port(0, 0, Port.OUTPUT, BitWidth.FLOAT);
        setPorts(ps);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.drawBounds();
        painter.drawPort(IN);
        painter.drawPort(OUT, "-x", Direction.WEST);
    }

    @Override
    public void propagate(InstanceState state) {
        // compute outputs
        Value a = state.getPortValue(IN);

        Value out;
        if(a.isFullyDefined()) {
            try {
                out = Value.createFloat(-a.toFloatValue());
            } catch(ArithmeticException e) {
                out = Value.ERROR;
            }
        } else if(a.isErrorValue()){
            out = Value.createError(BitWidth.FLOAT);
        } else {
            out = Value.createUnknown(BitWidth.FLOAT);
        }

        // propagate them
        int delay = 1;
        state.setPort(OUT, out, delay);
    }
}
