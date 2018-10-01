package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.std.arith.Strings;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsRenderer;

import java.awt.*;

public class MiniBitSelector extends InstanceFactory {
    private static final Attribute<BitWidth> ATTR_IN_WIDTH = Attributes
            .forBitWidth("in_width", Strings.getter("miniSelectorInAttr"));
    private static final Attribute<BitWidth> ATTR_OUT_WIDTH = Attributes
            .forBitWidth("out_width", Strings.getter("miniSelectorOutAttr"));
    private static final Attribute<Integer> ATTR_FIRST_BIT = Attributes
            .forIntegerRange("first_bit", Strings.getter("miniSelectorFirstBitAttr"), 0, 31);
    public MiniBitSelector() {
        super("Mini Bit Selector", Strings.getter("miniSelectorComponent"));
        setAttributes(new Attribute[] {
                StdAttr.FACING, ATTR_IN_WIDTH, ATTR_OUT_WIDTH, ATTR_FIRST_BIT
        }, new Object[] {
                Direction.EAST, BitWidth.create(16), BitWidth.create(8), 0
        });

        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(JoinedConfigurator.create(new BitWidthConfigurator(
                ATTR_OUT_WIDTH), new BitWidthConfigurator(ATTR_IN_WIDTH, 1,
                Value.MAX_WIDTH, 0)));
        setIconName("miniBitSelector.gif");
    }

    //
    // methods for instances
    //
    @Override
    protected void configureNewInstance(Instance instance) {
        configurePorts(instance);
        instance.addAttributeListener();
    }

    private void configurePorts(Instance instance) {
        Direction facing = instance.getAttributeValue(StdAttr.FACING);
        int dx = -30;

        Port[] ports = new Port[2];
        ports[0] = new Port(0, 0, Port.OUTPUT, ATTR_OUT_WIDTH);
        Location out = Location.create(0, 0).translate(facing, dx);
        ports[1] = new Port(out.getX(), out.getY(), Port.INPUT, ATTR_IN_WIDTH);
        instance.setPorts(ports);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        Direction facing = attrs.getValue(StdAttr.FACING);
        if (facing == Direction.SOUTH)
            return Bounds.create(-10, -30, 20, 30);
        if (facing == Direction.NORTH)
            return Bounds.create(-10, 0, 20, 30);
        if (facing == Direction.WEST)
            return Bounds.create(0, -10, 30, 20);
        return Bounds.create(-30, -10, 30, 20);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == StdAttr.FACING || attr == ATTR_FIRST_BIT) {
            instance.recomputeBounds();
            configurePorts(instance);
            instance.fireInvalidated();
        }
    }

    private void paintBase(InstancePainter painter) {
        Graphics graphics = painter.getGraphics();
        Direction facing = painter.getAttributeValue(StdAttr.FACING);
        Location loc = painter.getLocation();
        int x = loc.getX();
        int y = loc.getY();
        graphics.translate(x, y);
        double rotate = 0.0;
        if (facing != null && facing != Direction.EAST
                && graphics instanceof Graphics2D) {
            rotate = -facing.toRadians();
            ((Graphics2D) graphics).rotate(rotate);
        }

        GraphicsRenderer g = new GraphicsRenderer(graphics, 0, 0);
        g.switchToWidth(2);
        g.run("m 0,-5 l -15,0 l -5,-5 l -10,0 l 0,20 l 10,0 l 5,-5 l 15,0 l 0,-10");

        if (rotate != 0.0) {
            ((Graphics2D) graphics).rotate(-rotate);
        }
        graphics.translate(-x, -y);
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        paintBase(painter);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.getGraphics().setColor(Color.BLACK);
        paintBase(painter);

        Direction facing = painter.getInstance().getAttributeValue(StdAttr.FACING);
        if(facing == Direction.NORTH || facing == Direction.SOUTH) facing = facing.reverse();

        painter.drawPort(0, "", facing.reverse());
        painter.drawPort(1, "", facing);

        painter.drawLabel();
    }

    @Override
    public void propagate(InstanceState state) {
        Value in = state.getPortValue(1);
        BitWidth wout = state.getAttributeValue(ATTR_OUT_WIDTH);
        if(in.isFullyDefined()) {
            long value = in.toLongValue();
            value = value >> state.getAttributeValue(ATTR_FIRST_BIT);
            value = value & (1L << wout.getWidth() - 1);
            state.setPort(0, Value.createKnown(wout, (int)value), 1);
        } else if(in.isUnknown()) {
            state.setPort(0, Value.createUnknown(wout), 1);
        } else {
            state.setPort(0, Value.createError(wout), 1);
        }
    }
}
