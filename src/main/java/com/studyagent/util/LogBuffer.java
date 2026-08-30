package com.studyagent.util;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class LogBuffer {

    private final Deque<String> lines;
    private final int capacity;

    public LogBuffer(int capacity) {
        this.capacity = capacity;
        this.lines = new ArrayDeque<>(capacity);
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    public synchronized void append(String line) {
        lines.addLast("[" + LocalDateTime.now().format(TS) + "] " + line);
        while (lines.size() > capacity) {
            lines.removeFirst();
        }
    }

    public synchronized List<String> snapshot() {
        return new ArrayList<>(lines);
    }

    public void install() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        PrintStream teeOut = new PrintStream(new TeeOutputStream(originalOut, this), true, StandardCharsets.UTF_8);
        PrintStream teeErr = new PrintStream(new TeeOutputStream(originalErr, this), true, StandardCharsets.UTF_8);

        System.setOut(teeOut);
        System.setErr(teeErr);
    }
}
