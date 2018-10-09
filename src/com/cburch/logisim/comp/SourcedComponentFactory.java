package com.cburch.logisim.comp;

import com.cburch.logisim.data.AttributeSet;

import java.io.File;
import java.io.IOException;

public interface SourcedComponentFactory {
    void reloadFromSource(File file, AttributeSet attributeSet) throws IOException;
}
