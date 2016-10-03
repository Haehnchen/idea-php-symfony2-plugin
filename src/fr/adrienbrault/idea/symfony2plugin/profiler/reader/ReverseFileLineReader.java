package fr.adrienbrault.idea.symfony2plugin.profiler.reader;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Rebuild ReversedLinesFileReader
 *
 * @link http://stackoverflow.com/questions/6011345/read-a-file-line-by-line-in-reverse-order
 */
public class ReverseFileLineReader {
    private static final int BUFFER_SIZE = 8192;
    private int limit;
    private FileChannel channel;
    private final String encoding;
    private long filePos;
    private MappedByteBuffer buf;
    private int bufPos;
    private byte lastLineBreak = '\n';
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final RandomAccessFile raf;

    public ReverseFileLineReader(@NotNull File file, @NotNull String encoding, int limit) throws IOException {
        this.limit = limit;
        raf = new RandomAccessFile(file, "r");
        channel = raf.getChannel();
        filePos = raf.length();
        this.encoding = encoding;
    }

    public String[] readLines() throws IOException {
        List<String> lines = new ArrayList<>();

        String line;
        while ((line = readLine()) != null && limit-- > 0) {
            lines.add(line);
        }

        channel.close();
        raf.close();

        return lines.toArray(new String[lines.size()]);
    }

    private String readLine() throws IOException {
        while (true) {
            if (bufPos < 0) {
                if (filePos == 0) {
                    if (baos == null) {
                        return null;
                    }
                    String line = bufToString();
                    baos = null;
                    return line;
                }

                long start = Math.max(filePos - BUFFER_SIZE, 0);
                long end = filePos;
                long len = end - start;

                buf = channel.map(FileChannel.MapMode.READ_ONLY, start, len);
                bufPos = (int) len;
                filePos = start;
            }

            while (bufPos-- > 0) {
                byte c = buf.get(bufPos);
                if (c == '\r' || c == '\n') {
                    if (c != lastLineBreak) {
                        lastLineBreak = c;
                        continue;
                    }
                    lastLineBreak = c;
                    return bufToString();
                }
                baos.write(c);
            }
        }
    }

    private String bufToString() throws UnsupportedEncodingException {
        if (baos.size() == 0) {
            return "";
        }

        byte[] bytes = baos.toByteArray();
        for (int i = 0; i < bytes.length / 2; i++) {
            byte t = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = t;
        }

        baos.reset();

        return new String(bytes, encoding);
    }
}