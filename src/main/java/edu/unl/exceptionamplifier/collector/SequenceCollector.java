package edu.unl.exceptionamplifier.collector;

import java.util.ArrayList;
import java.util.List;

public class SequenceCollector {
    private final List<String> sequence = new ArrayList<>();

    public void collect(String apiCall) {
        sequence.add(apiCall);
    }

    public List<String> getSequence() {
        return new ArrayList<>(sequence);
    }
}
