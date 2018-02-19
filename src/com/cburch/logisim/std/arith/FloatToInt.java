package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;

public class FloatToInt extends InstanceFactory {
    static final int PER_DELAY = 1;
    private static final int IN = 0;
    private static final int OUT = 1;

    public FloatToInt() {
        super("FloatToInt", Strings.getter("floatToIntComponent"));
        setAttributes(new Attribute[] { StdAttr.WIDTH },
                new Object[] { BitWidth.create(8) });
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));

        Port[] ps = new Port[2];
        ps[IN] = new Port(-40, 0, Port.INPUT, BitWidth.FLOAT);
        ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
        setPorts(ps);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.drawBounds();
        painter.drawPort(IN, "F", Direction.EAST);
        painter.drawPort(OUT, "I", Direction.WEST);
    }

    @Override
    public void propagate(InstanceState state) {
        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        // compute outputs
        Value a = state.getPortValue(IN);

        Value out;
        if(a.isFullyDefined()) {
            out = Value.createKnown(width, (int)a.toFloatValue());
        } else if(a.isErrorValue()){
            out = Value.createError(width);
        } else {
            out = Value.createUnknown(width);
        }

        // propagate them
        int delay = 1;
        state.setPort(OUT, out, delay);
    }
}
