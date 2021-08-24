package com.futurewei.alcor.portmanager.util;

import com.futurewei.alcor.portmanager.processor.IProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessorState {
    public List<IProcessor> processors = new ArrayList<>();
    public Map<Class, IProcessor> processorMap = new HashMap<>();
}
