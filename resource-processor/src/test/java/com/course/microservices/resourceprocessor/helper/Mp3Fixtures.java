package com.course.microservices.resourceprocessor.helper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class Mp3Fixtures {

    private static final int FRAME_SIZE = 417;

    private Mp3Fixtures() {
    }

    public static byte[] song() {
        return mp3("Yesterday", "The Beatles", "Help!", "1965", 96);
    }

    public static byte[] mp3(String title, String artist, String album, String year, int frames) {
        ByteArrayOutputStream tagFrames = new ByteArrayOutputStream();
        writeTextFrame(tagFrames, "TIT2", title);
        writeTextFrame(tagFrames, "TPE1", artist);
        writeTextFrame(tagFrames, "TALB", album);
        writeTextFrame(tagFrames, "TYER", year);
        byte[] tag = tagFrames.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("ID3".getBytes(StandardCharsets.US_ASCII));
        out.write(3);
        out.write(0);
        out.write(0);
        out.writeBytes(syncSafe(tag.length));
        out.writeBytes(tag);
        for (int i = 0; i < frames; i++) {
            byte[] frame = new byte[FRAME_SIZE];
            frame[0] = (byte) 0xFF;
            frame[1] = (byte) 0xFB;
            frame[2] = (byte) 0x90;
            frame[3] = 0x64;
            out.writeBytes(frame);
        }
        return out.toByteArray();
    }

    private static void writeTextFrame(ByteArrayOutputStream out, String id, String text) {
        byte[] value = text.getBytes(StandardCharsets.ISO_8859_1);
        int size = value.length + 1;
        out.writeBytes(id.getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(new byte[]{(byte) (size >> 24), (byte) (size >> 16), (byte) (size >> 8), (byte) size});
        out.writeBytes(new byte[]{0, 0});
        out.write(0);
        out.writeBytes(value);
    }

    private static byte[] syncSafe(int value) {
        return new byte[]{
                (byte) ((value >> 21) & 0x7F),
                (byte) ((value >> 14) & 0x7F),
                (byte) ((value >> 7) & 0x7F),
                (byte) (value & 0x7F)};
    }
}
