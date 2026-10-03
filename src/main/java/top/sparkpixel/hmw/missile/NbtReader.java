package top.sparkpixel.hmw.missile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Minimal binary NBT reader for vanilla structure template files.
 * Produces plain Java object graphs: Map&lt;String,Object&gt; compounds,
 * List&lt;Object&gt; lists, String, Byte/Short/Integer/Long/Float/Double,
 * byte[]/int[]/long[].
 */
final class NbtReader {

    private NbtReader() {
    }

    /** Reads the root compound of an NBT file (gzip or raw). */
    @SuppressWarnings("unchecked")
    static Map<String, Object> read(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        DataInputStream in;
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B) {
            in = new DataInputStream(new BufferedInputStream(
                    new GZIPInputStream(new ByteArrayInputStream(bytes))));
        } else {
            in = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes)));
        }
        try (in) {
            int type = in.readByte();
            if (type != 10) {
                throw new IOException("Not a compound NBT root (tag " + type + "): " + file.getName());
            }
            in.readUTF(); // root name
            return (Map<String, Object>) readPayload(in, type);
        }
    }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> {
                int length = in.readInt();
                byte[] array = new byte[length];
                in.readFully(array);
                yield array;
            }
            case 8 -> in.readUTF();
            case 9 -> {
                int elementType = in.readByte();
                int length = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, length));
                for (int i = 0; i < length; i++) {
                    list.add(elementType == 0 ? null : readPayload(in, elementType));
                }
                yield list;
            }
            case 10 -> {
                Map<String, Object> compound = new HashMap<>();
                while (true) {
                    int childType = in.readByte();
                    if (childType == 0) {
                        break;
                    }
                    String name = in.readUTF();
                    compound.put(name, readPayload(in, childType));
                }
                yield compound;
            }
            case 11 -> {
                int length = in.readInt();
                int[] array = new int[length];
                for (int i = 0; i < length; i++) {
                    array[i] = in.readInt();
                }
                yield array;
            }
            case 12 -> {
                int length = in.readInt();
                long[] array = new long[length];
                for (int i = 0; i < length; i++) {
                    array[i] = in.readLong();
                }
                yield array;
            }
            default -> throw new IOException("Unknown NBT tag type " + type);
        };
    }
}
