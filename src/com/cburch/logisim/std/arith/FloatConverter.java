package com.cburch.logisim.std.arith;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.std.gates.PainterShaped;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

import javax.swing.*;
import java.awt.*;

public class FloatConverter extends InstanceFactory {
    private static final AttributeOption INPUT_INTEGER = new AttributeOption("int",
            Strings.getter("inputInteger"));
    private static final AttributeOption INPUT_FLOAT = new AttributeOption(
            "float", Strings.getter("inputFloat"));
    private static final Attribute<AttributeOption> ATTR_INPUT_TYPE = Attributes
            .forOption("inputtype", Strings.getter("converterInputTypeAttr"),
                    new AttributeOption[] { INPUT_INTEGER, INPUT_FLOAT });

    private static final Icon toolIcon = Icons.getIcon("notGate.gif");

    public FloatConverter() {
        super("FloatConverter", Strings.getter("floatConverterComponent"));
        setAttributes(new Attribute[] {
                StdAttr.FACING, StdAttr.WIDTH, ATTR_INPUT_TYPE
        }, new Object[] {
                Direction.EAST, BitWidth.ONE, INPUT_INTEGER
        });
        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
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
        AttributeOption type = instance.getAttributeValue(ATTR_INPUT_TYPE);
        BitWidth width = instance.getAttributeValue(StdAttr.WIDTH);
        int dx = -30;

        BitWidth inWidth, outWidth;
        if(type == INPUT_INTEGER) {
            inWidth = width;
            outWidth = BitWidth.FLOAT;
        } else {
            inWidth = BitWidth.FLOAT;
            outWidth = width;
        }

        Port[] ports = new Port[2];
        ports[0] = new Port(0, 0, Port.OUTPUT, outWidth);
        Location out = Location.create(0, 0).translate(facing, dx);
        ports[1] = new Port(out.getX(), out.getY(), Port.INPUT, inWidth);
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
        if (attr == StdAttr.FACING) {
            instance.recomputeBounds();
            configurePorts(instance);
        }
    }

    private void paintBase(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Direction facing = painter.getAttributeValue(StdAttr.FACING);
        Location loc = painter.getLocation();
        int x = loc.getX();
        int y = loc.getY();
        g.translate(x, y);
        double rotate = 0.0;
        if (facing != null && facing != Direction.EAST
                && g instanceof Graphics2D) {
            rotate = -facing.toRadians();
            ((Graphics2D) g).rotate(rotate);
        }

        int[] xp = new int[4];
        int[] yp = new int[4];
        xp[0] = -10;
        yp[0] = -10;

        xp[1] = -29;
        yp[1] = -10;

        xp[2] = -29;
        yp[2] =  10;

        xp[3] = -10;
        yp[3] =  10;

        g.drawPolyline(xp, yp, 4);
        g.drawArc(-20, -10, 20, 20, -90, 180);

        if (rotate != 0.0) {
            ((Graphics2D) g).rotate(-rotate);
        }
        g.translate(-x, -y);
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        paintBase(painter);
    }

    //
    // painting methods
    //
    @Override
    public void paintIcon(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        g.setColor(Color.black);
            if (toolIcon != null) {
                toolIcon.paintIcon(painter.getDestination(), g, 2, 2);
            } else {
                int[] xp = new int[4];
                int[] yp = new int[4];
                xp[0] = 15;
                yp[0] = 10;
                xp[1] = 1;
                yp[1] = 3;
                xp[2] = 1;
                yp[2] = 17;
                xp[3] = 15;
                yp[3] = 10;
                g.drawPolyline(xp, yp, 4);
                g.drawOval(15, 8, 4, 4);
            }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.getGraphics().setColor(Color.BLACK);
        paintBase(painter);

        AttributeOption type = painter.getInstance().getAttributeValue(ATTR_INPUT_TYPE);
        Direction facing = painter.getInstance().getAttributeValue(StdAttr.FACING);
        if(facing == Direction.NORTH || facing == Direction.SOUTH) facing = facing.reverse();

        painter.drawPort(0, type == INPUT_INTEGER ? " F " : " I ", facing.reverse());
        painter.drawPort(1, type == INPUT_INTEGER ? " I " : " F ", facing);

        painter.drawLabel();
    }

    @Override
    public void propagate(InstanceState state) {
        AttributeOption type = state.getAttributeValue(ATTR_INPUT_TYPE);
        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        Value in = state.getPortValue(1);
        Value out;
        if(in.isErrorValue()) {
            out = Value.createError(type == INPUT_INTEGER ? width : BitWidth.FLOAT);
        } else if(in.isUnknown()) {
            out = Value.createUnknown(type == INPUT_INTEGER ? width : BitWidth.FLOAT);
        } else if(in.isFullyDefined()) {
            if(type == INPUT_INTEGER) {
                out = Value.createFloat((float)in.toIntValue());
            } else {
                out = Value.createKnown(width, (int)in.toFloatValue());
            }
        } else {
            out = Value.createError(type == INPUT_INTEGER ? width : BitWidth.FLOAT);
        }

        state.setPort(0, out, 1);
    }
}
