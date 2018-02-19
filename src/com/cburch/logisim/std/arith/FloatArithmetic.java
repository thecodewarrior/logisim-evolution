package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

import java.awt.*;

public abstract class FloatArithmetic extends InstanceFactory {
    static final int PER_DELAY = 1;
    private static final int IN0 = 0;
    private static final int IN1 = 1;
    private static final int OUT = 2;

    public FloatArithmetic(String name, StringGetter displayName) {
        super(name, displayName);
        setAttributes(new Attribute[] {}, new Object[] {});
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));

        Port[] ps = new Port[3];
        ps[IN0] = new Port(-40, -10, Port.INPUT, BitWidth.FLOAT);
        ps[IN1] = new Port(-40, 10, Port.INPUT, BitWidth.FLOAT);
        ps[OUT] = new Port(0, 0, Port.OUTPUT, BitWidth.FLOAT);
        setPorts(ps);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        painter.drawBounds();

        g.setColor(Color.GRAY);
        painter.drawPort(IN0);
        painter.drawPort(IN1);
        painter.drawPort(OUT);

        Location loc = painter.getLocation();
        int x = loc.getX();
        int y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        drawIcon(g, x, y);
        GraphicsUtil.switchToWidth(g, 1);
    }

    @Override
    public void propagate(InstanceState state) {
        // compute outputs
        Value a = state.getPortValue(IN0);
        Value b = state.getPortValue(IN1);

        Value out;
        if(a.isFullyDefined() && b.isFullyDefined()) {
            try {
                out = Value.createFloat(perform(a.toFloatValue(), b.toFloatValue()));
            } catch(ArithmeticException e) {
                out = Value.ERROR;
            }
        } else if(a.isErrorValue() || b.isErrorValue()){
            out = Value.createError(BitWidth.FLOAT);
        } else {
            out = Value.createUnknown(BitWidth.FLOAT);
        }

        // propagate them
        int delay = 16 * PER_DELAY; // Figure out how long these things actually take
        state.setPort(OUT, out, delay);
    }

    public abstract float perform(float inputA, float inputB);
    public abstract void drawIcon(Graphics g, int x, int y);
}
