package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.util.StringGetter;

public class FloatExtractor extends InstanceFactory {
    static final int PER_DELAY = 1;
    private static final int IN = 0;
    private static final int OUT = 1;

    private static final AttributeOption PART_SIGN = new AttributeOption("sign",
            Strings.getter("arithOutputSign"));
    private static final AttributeOption PART_EXPONENT = new AttributeOption("exponent",
            Strings.getter("arithOutputExponent"));
    private static final AttributeOption PART_SIGNIFICAND = new AttributeOption("significand",
            Strings.getter("arithOutputSignificand"));
    private static final Attribute<AttributeOption> ATTR_SELECT_OUTPUT = Attributes
            .forOption("floatoutput", Strings.getter("arithFloatExtractComponent"),
                    new AttributeOption[] { PART_SIGN, PART_EXPONENT, PART_SIGNIFICAND });

    private static final BitWidth EXPONENT_WIDTH = BitWidth.create(8);
    private static final BitWidth SIGNIFICAND_WIDTH = BitWidth.create(23);

    public FloatExtractor() {
        super("FloatExtractor", Strings.getter("floatExtractorComponent"));
        setAttributes(new Attribute[] { ATTR_SELECT_OUTPUT }, new Object[] { PART_SIGN });
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        updatePorts(instance);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        updatePorts(instance);
    }

    private void updatePorts(Instance instance) {
        AttributeOption option = instance.getAttributeValue(ATTR_SELECT_OUTPUT);

        BitWidth width = BitWidth.ONE;
        if(option == PART_SIGN) {
            width = BitWidth.ONE;
        } else if(option == PART_EXPONENT) {
            width = EXPONENT_WIDTH;
        } else if(option == PART_SIGNIFICAND) {
            width = SIGNIFICAND_WIDTH;
        }

        Port[] ps = new Port[2];
        ps[IN] = new Port(-40, 0, Port.INPUT, BitWidth.FLOAT);
        ps[OUT] = new Port(0, 0, Port.OUTPUT, width);
        instance.setPorts(ps);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        AttributeOption output = painter.getInstance().getAttributeValue(ATTR_SELECT_OUTPUT);
        String label = "";
        if(output == PART_SIGN) {
            label = "-";
        } else if(output == PART_EXPONENT){
            label = "exp";
        } else if(output == PART_SIGNIFICAND){
            label = "sgn";
        }
        painter.drawBounds();
        painter.drawPort(IN);
        painter.drawPort(OUT, label, Direction.WEST);
    }

    @Override
    public void propagate(InstanceState state) {
        AttributeOption output = state.getAttributeValue(ATTR_SELECT_OUTPUT);

        // compute outputs
        Value a = state.getPortValue(IN);

        Value out = null;
        if(a.isFullyDefined()) {
            int v = a.toIntValue();
            if(output == PART_SIGN) {
                out = (v & 0x80000000L) > 0 ? Value.TRUE : Value.FALSE;
            } else if(output == PART_EXPONENT) {
                out = Value.createKnown(EXPONENT_WIDTH, (v & 0x7f800000) >> 23);
            } else if(output == PART_SIGNIFICAND) {
                out = Value.createKnown(SIGNIFICAND_WIDTH, (v & 0x007fffff));
            }
        } else if(a.isErrorValue()){
            if(output == PART_SIGN) {
                out = Value.ERROR;
            } else if(output == PART_EXPONENT) {
                out = Value.createError(EXPONENT_WIDTH);
            } else if(output == PART_SIGNIFICAND) {
                out = Value.createError(SIGNIFICAND_WIDTH);
            }
        } else {
            if(output == PART_SIGN) {
                out = Value.UNKNOWN;
            } else if(output == PART_EXPONENT) {
                out = Value.createUnknown(EXPONENT_WIDTH);
            } else if(output == PART_SIGNIFICAND) {
                out = Value.createUnknown(SIGNIFICAND_WIDTH);
            }
        }

        if(out == null) {
            return;
        }

        // propagate them
        int delay = 1;
        state.setPort(OUT, out, delay);
    }
}
