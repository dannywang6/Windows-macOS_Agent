package com.studyagent.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TeeOutputStream extends OutputStream {

    private final OutputStream target;
    private final LogBuffer buffer;
    private final StringBuilder lineBuffer = new StringBuilder();

    public TeeOutputStream(OutputStream target, LogBuffer buffer) {
        this.target = target;
        this.buffer = buffer;
    }

    @Override
    public void write(int b) throws IOException {
        target.write(b);
        lineBuffer.append((char) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        target.write(b, off, len);
        lineBuffer.append(new String(b, off, len, StandardCharsets.UTF_8));
        flushLines();
    }

    @Override
    public void flush() throws IOException {
        target.flush();
    }

    @Override
    public void close() throws IOException {
        emitLine();
        target.close();
    }

    private void flushLines() {
        int idx;
        while ((idx = lineBuffer.indexOf("\n")) >= 0) {
            String line = lineBuffer.substring(0, idx);
            if (!line.isEmpty()) {
                buffer.append(line);
            }
            lineBuffer.delete(0, idx + 1);
        }
    }

    private void emitLine() {
        if (lineBuffer.length() > 0) {
            buffer.append(lineBuffer.toString());
            lineBuffer.setLength(0);
        }
    }
}
