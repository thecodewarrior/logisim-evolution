package com.cburch.logisim.util;

public class Vec2i {
    int x;
    int y;

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vec2i clone() {
        return new Vec2i(x, y);
    }

    public void copyFrom(Vec2i other) {
        x = other.x;
        y = other.y;
    }
}
