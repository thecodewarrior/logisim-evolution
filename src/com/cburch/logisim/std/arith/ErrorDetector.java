package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.Icons;

import javax.swing.*;
import java.awt.*;

public class ErrorDetector extends InstanceFactory {
    private static final Icon toolIcon = Icons.getIcon("notGate.gif");

    public ErrorDetector() {
        super("ErrorDetector", Strings.getter("errorDetectorComponent"));
        setAttributes(new Attribute[] {
                StdAttr.FACING, StdAttr.WIDTH
        }, new Object[] {
                Direction.EAST, BitWidth.ONE
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
        BitWidth width = instance.getAttributeValue(StdAttr.WIDTH);
        int dx = -30;

        Port[] ports = new Port[2];
        ports[0] = new Port(0, 0, Port.OUTPUT, 1);
        Location out = Location.create(0, 0).translate(facing, dx);
        ports[1] = new Port(out.getX(), out.getY(), Port.INPUT, width);
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
        if (attr == StdAttr.FACING || attr == StdAttr.WIDTH) {
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

        Direction facing = painter.getInstance().getAttributeValue(StdAttr.FACING);
        if(facing == Direction.NORTH || facing == Direction.SOUTH) facing = facing.reverse();

        painter.drawPort(0, "", facing.reverse());
        painter.drawPort(1, "!!", facing);

        painter.drawLabel();
    }

    @Override
    public void propagate(InstanceState state) {
        Value in = state.getPortValue(1);
        Value out;
        if(in.isErrorValue()) {
            out = Value.TRUE;
        } else {
            out = Value.UNKNOWN;
        }

        state.setPort(0, out, 1);
    }
}
