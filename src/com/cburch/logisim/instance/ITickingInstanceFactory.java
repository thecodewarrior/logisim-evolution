package com.cburch.logisim.instance;

public interface ITickingInstanceFactory {
    /**
     *
     * @return true if state changes were made
     */
    boolean tick(InstanceState state);
}
