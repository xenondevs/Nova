package xyz.xenondevs.nova.resources.builder.font.provider.unihex;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BiConsumer;

public record UnihexGlyphs(
    @NotNull Int2ObjectOpenHashMap<int[]> glyphs8,
    @NotNull Int2ObjectOpenHashMap<int[]> glyphs16,
    @NotNull Int2ObjectOpenHashMap<int[]> glyphs24,
    @NotNull Int2ObjectOpenHashMap<int[]> glyphs32
)
{
    
    private static final byte[] HEX_CHARS = new byte[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F'
    };
    
    public UnihexGlyphs() {
        this(new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
    }
    
    /**
     * Reads a unihex file from a {@link Path}.
     *
     * @param path The path to read from
     * @return The glyphs
     * @throws IOException If an I/O error occurs
     */
    public static @NotNull UnihexGlyphs readUnihexFile(@NotNull Path path) throws IOException {
        byte[] bin = Files.readAllBytes(path);
        return readUnihexFile(bin);
    }
    
    /**
     * Reads a unihex file from a byte array.
     *
     * @param bin The byte array to read from
     * @return The glyphs
     * @throws IOException If an I/O error occurs
     */
    public static @NotNull UnihexGlyphs readUnihexFile(byte @NotNull [] bin) throws IOException {
        return readUnihexFile(new ByteArrayInputStream(bin));
    }
    
    /**
     * Reads a unihex file from an {@link InputStream}.
     *
     * @param in The input stream to read from
     * @return The glyphs
     * @throws IOException If an I/O error occurs
     */
    public static @NotNull UnihexGlyphs readUnihexFile(@NotNull InputStream in) throws IOException {
        var glyphs8 = new Int2ObjectOpenHashMap<int[]>();
        var glyphs16 = new Int2ObjectOpenHashMap<int[]>();
        var glyphs24 = new Int2ObjectOpenHashMap<int[]>();
        var glyphs32 = new Int2ObjectOpenHashMap<int[]>();
        
        var codePoint = 0;
        var buffer = new ByteArrayList();
        
        int i;
        while ((i = in.read()) != -1) {
            if (i == ':') {
                int size = buffer.size();
                if (size != 4 && size != 5 && size != 6)
                    throw new IllegalArgumentException("Code point hex must be 4, 5, or 6 characters long");
                
                for (int j = 0; j < size; j++) {
                    codePoint = codePoint << 4 | fromHex(buffer.getByte(j));
                }
                
                buffer.clear();
            } else if (i == '\n') {
                switch (buffer.size()) {
                    case 32 -> glyphs8.put(codePoint, read8(buffer.toByteArray()));
                    case 64 -> glyphs16.put(codePoint, read16(buffer.toByteArray()));
                    case 96 -> glyphs24.put(codePoint, read24(buffer.toByteArray()));
                    case 128 -> glyphs32.put(codePoint, read32(buffer.toByteArray()));
                }
                
                buffer.clear();
                codePoint = 0;
            } else {
                buffer.add((byte) i);
            }
        }
        
        return new UnihexGlyphs(glyphs8, glyphs16, glyphs24, glyphs32);
    }
    
    /**
     * Writes these unihex glyphs to a {@link Path}.
     *
     * @param path The path to write to
     * @throws IOException If an I/O error occurs
     */
    public void writeUnihexFile(@NotNull Path path) throws IOException {
        Files.write(path, writeUnihexFile());
    }
    
    /**
     * Writes these unihex glyphs to a byte array.
     *
     * @return The byte array
     * @throws IOException If an I/O error occurs
     */
    public byte @NotNull [] writeUnihexFile() throws IOException {
        var out = new ByteArrayOutputStream();
        writeUnihexFile(out);
        return out.toByteArray();
    }
    
    /**
     * Writes these unihex glyphs to an {@link OutputStream}.
     *
     * @param out The output stream to write to
     * @throws IOException If an I/O error occurs
     */
    public void writeUnihexFile(@NotNull OutputStream out) throws IOException {
        writeUnihexFile(out, glyphs8, UnihexGlyphs::write8);
        writeUnihexFile(out, glyphs16, UnihexGlyphs::write16);
        writeUnihexFile(out, glyphs24, UnihexGlyphs::write24);
        writeUnihexFile(out, glyphs32, UnihexGlyphs::write32);
    }
    
    private void writeUnihexFile(@NotNull OutputStream out, @NotNull Int2ObjectMap<int @NotNull []> glyphs, @NotNull BiConsumer<@NotNull OutputStream, int @NotNull []> writer) throws IOException {
        for (var glyph : glyphs.int2ObjectEntrySet()) {
            writeCodePoint(out, glyph.getIntKey());
            out.write(':');
            writer.accept(out, glyph.getValue());
            out.write('\n');
        }
    }
    
    /**
     * Merges all glyphs from another {@link UnihexGlyphs} into this one.
     *
     * @param other The other glyphs
     */
    public void merge(@NotNull UnihexGlyphs other) {
        glyphs8.putAll(other.glyphs8);
        glyphs16.putAll(other.glyphs16);
        glyphs24.putAll(other.glyphs24);
        glyphs32.putAll(other.glyphs32);
    }
    
    /**
     * Reads a 8x16 glyph from a byte array of length 32, where each byte is a hex digit.
     *
     * @param bin The byte array
     * @return The glyph as an int array, where each int is a row in the glyph texture.
     * Only the first 8 least significant bits of each int are used, where the most significant bit is the leftmost pixel.
     */
    public static int @NotNull [] read8(byte @NotNull [] bin) {
        int[] glyph = new int[16];
        
        for (int i = 0; i < 16; i++) {
            int binIdx = i * 2;
            glyph[i] = fromHex(bin[binIdx]) << 4 | fromHex(bin[binIdx + 1]);
        }
        
        return glyph;
    }
    
    /**
     * Reads a 16x16 glyph from a byte array of length 64, where each byte is a hex digit.
     *
     * @param bin The byte array
     * @return The glyph as an int array, where each int is a row in the glyph texture.
     * Only the first 16 least significant bits of each int are used, where the most significant bit is the leftmost pixel.
     */
    public static int @NotNull [] read16(byte @NotNull [] bin) {
        int[] glyph = new int[16];
        
        for (int i = 0; i < 16; i++) {
            int binIdx = i * 4;
            glyph[i] = fromHex(bin[binIdx]) << 12
                       | fromHex(bin[binIdx + 1]) << 8
                       | fromHex(bin[binIdx + 2]) << 4
                       | fromHex(bin[binIdx + 3]);
        }
        
        return glyph;
    }
    
    /**
     * Reads a 24x16 glyph from a byte array of length 96, where each byte is a hex digit.
     *
     * @param bin The byte array
     * @return The glyph as an int array, where each int is a row in the glyph texture.
     * Only the first 24 least significant bits of each int are used, where the most significant bit is the leftmost pixel.
     */
    public static int @NotNull [] read24(byte @NotNull [] bin) {
        int[] glyph = new int[16];
        
        for (int i = 0; i < 16; i++) {
            int binIdx = i * 6;
            glyph[i] = fromHex(bin[binIdx]) << 20
                       | fromHex(bin[binIdx + 1]) << 16
                       | fromHex(bin[binIdx + 2]) << 12
                       | fromHex(bin[binIdx + 3]) << 8
                       | fromHex(bin[binIdx + 4]) << 4
                       | fromHex(bin[binIdx + 5]);
        }
        
        return glyph;
    }
    
    /**
     * Reads a 32x16 glyph from a byte array of length 128, where each byte is a hex digit.
     *
     * @param bin The byte array
     * @return The glyph as an int array, where each int is a row in the glyph texture.
     * The most significant of each int is the leftmost pixel.
     */
    public static int @NotNull [] read32(byte @NotNull [] bin) {
        int[] glyph = new int[16];
        
        for (int i = 0; i < 16; i++) {
            int binIdx = i * 8;
            glyph[i] = fromHex(bin[binIdx]) << 28
                       | fromHex(bin[binIdx + 1]) << 24
                       | fromHex(bin[binIdx + 2]) << 20
                       | fromHex(bin[binIdx + 3]) << 16
                       | fromHex(bin[binIdx + 4]) << 12
                       | fromHex(bin[binIdx + 5]) << 8
                       | fromHex(bin[binIdx + 6]) << 4
                       | fromHex(bin[binIdx + 7]);
        }
        
        return glyph;
    }
    
    /**
     * Writes a 8x16 glyph to an output stream. This writes 32 bytes, where each byte is a hex digit.
     *
     * @param out   The output stream to write to
     * @param glyph The glyph as an int array, where each int is a row in the glyph texture.
     */
    public static void write8(@NotNull OutputStream out, int @NotNull [] glyph) {
        try {
            for (int i = 0; i < 16; i++) {
                int line = glyph[i];
                out.write(HEX_CHARS[line >>> 4 & 0xF]);
                out.write(HEX_CHARS[line & 0xF]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Writes a 16x16 glyph to an output stream. This writes 64 bytes, where each byte is a hex digit.
     *
     * @param out   The output stream to write to
     * @param glyph The glyph as an int array, where each int is a row in the glyph texture.
     */
    public static void write16(@NotNull OutputStream out, int @NotNull [] glyph) {
        try {
            for (int i = 0; i < 16; i++) {
                int line = glyph[i];
                out.write(HEX_CHARS[line >>> 12 & 0xF]);
                out.write(HEX_CHARS[line >>> 8 & 0xF]);
                out.write(HEX_CHARS[line >>> 4 & 0xF]);
                out.write(HEX_CHARS[line & 0xF]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Writes a 24x16 glyph to an output stream. This writes 96 bytes, where each byte is a hex digit.
     *
     * @param out   The output stream to write to
     * @param glyph The glyph as an int array, where each int is a row in the glyph texture.
     */
    public static void write24(@NotNull OutputStream out, int @NotNull [] glyph) {
        try {
            for (int i = 0; i < 16; i++) {
                int line = glyph[i];
                out.write(HEX_CHARS[line >>> 20 & 0xF]);
                out.write(HEX_CHARS[line >>> 16 & 0xF]);
                out.write(HEX_CHARS[line >>> 12 & 0xF]);
                out.write(HEX_CHARS[line >>> 8 & 0xF]);
                out.write(HEX_CHARS[line >>> 4 & 0xF]);
                out.write(HEX_CHARS[line & 0xF]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Writes a 32x16 glyph to an output stream. This writes 128 bytes, where each byte is a hex digit.
     *
     * @param out   The output stream to write to
     * @param glyph The glyph as an int array, where each int is a row in the glyph texture.
     */
    public static void write32(@NotNull OutputStream out, int @NotNull [] glyph) {
        try {
            for (int i = 0; i < 16; i++) {
                int line = glyph[i];
                out.write(HEX_CHARS[line >>> 28 & 0xF]);
                out.write(HEX_CHARS[line >>> 24 & 0xF]);
                out.write(HEX_CHARS[line >>> 20 & 0xF]);
                out.write(HEX_CHARS[line >>> 16 & 0xF]);
                out.write(HEX_CHARS[line >>> 12 & 0xF]);
                out.write(HEX_CHARS[line >>> 8 & 0xF]);
                out.write(HEX_CHARS[line >>> 4 & 0xF]);
                out.write(HEX_CHARS[line & 0xF]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Writes a code point in unihex format to an output stream.
     *
     * @param out       The output stream to write to
     * @param codePoint The code point
     */
    public static void writeCodePoint(@NotNull OutputStream out, int codePoint) {
        try {
            if (codePoint > 0xFFFFF)
                out.write(HEX_CHARS[codePoint >>> 20 & 0xF]);
            if (codePoint > 0xFFFF)
                out.write(HEX_CHARS[codePoint >>> 16 & 0xF]);
            out.write(HEX_CHARS[codePoint >>> 12 & 0xF]);
            out.write(HEX_CHARS[codePoint >>> 8 & 0xF]);
            out.write(HEX_CHARS[codePoint >>> 4 & 0xF]);
            out.write(HEX_CHARS[codePoint & 0xF]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates an argb raster from of glyph.
     *
     * @param width       The width of the binary glyph data.
     * @param glyph       The glyph as an int array, where each int is a row in the glyph texture.
     * @param leftBorder  The first column of the glyph, either determined by {@link UnihexGlyphs#findVerticalBorders(int, int[])}
     *                    or configured through size overrides.
     * @param rightBorder The last column of the glyph, either determined by {@link UnihexGlyphs#findVerticalBorders(int, int[])}
     *                    or configured through size overrides.
     * @param argb1       The color to use for pixels that are part of the glyph
     * @param argb0       The color to use for pixels that are not part of the glyph
     * @return An argb raster representing the image of the glyph.<br>
     * Dimensions: ({@code rightBorder - leftBorder + 1}) x {@code 16} pixels
     */
    public static int[] createArgbRaster(
        int width, int @NotNull [] glyph,
        int leftBorder, int rightBorder,
        int argb1, int argb0
    ) {
        int rasterWidth = rightBorder - leftBorder + 1;
        int[] raster = new int[rasterWidth * 16];
        Arrays.fill(raster, argb0);
        
        int endX = Math.min(width, rightBorder + 1); // exclusive
        for (int y = 0; y < 16; y++) {
            int line = glyph[y];
            for (int x = leftBorder; x < endX; x++) {
                if (((line >>> (width - x - 1)) & 1) == 1) {
                    raster[y * rasterWidth + x - leftBorder] = argb1;
                }
            }
        }
        
        return raster;
    }
    
    /**
     * Converts a hex digit to its integer value.
     *
     * @param hexDigit The hex digit
     * @return The integer value
     */
    private static int fromHex(byte hexDigit) {
        return switch (hexDigit) {
            case '0' -> 0;
            case '1' -> 1;
            case '2' -> 2;
            case '3' -> 3;
            case '4' -> 4;
            case '5' -> 5;
            case '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 9;
            case 'A', 'a' -> 10;
            case 'B', 'b' -> 11;
            case 'C', 'c' -> 12;
            case 'D', 'd' -> 13;
            case 'E', 'e' -> 14;
            case 'F', 'f' -> 15;
            default -> throw new IllegalArgumentException("Invalid hex digit: " + hexDigit);
        };
    }
    
    /**
     * Finds the first and last non-empty glyph in a glyph.
     *
     * @param glyph The glyph
     * @return An int array of length 2, where the first element is the index of the first non-empty line and the second element is the index of the last non-empty line.
     * If the glyph is completely empty, null is returned.
     */
    public static int @Nullable [] findHorizontalBorders(int @NotNull [] glyph) {
        int top = -1;
        for (int i = 0; i < 16; i++) {
            if (glyph[i] != 0) {
                top = i;
                break;
            }
        }
        
        if (top == -1)
            return null;
        
        int bottom = -1;
        for (int i = 15; i >= 0; i--) {
            if (glyph[i] != 0) {
                bottom = i;
                break;
            }
        }
        
        return new int[] {top, bottom};
    }
    
    
    /**
     * Finds the first and last non-empty columns in a glyph.
     *
     * @param width The width of the glyph
     * @param glyph The glyph
     * @return An int array of length 2, where the first element is the index of the first non-empty column and the second element is the index of the last non-empty column.
     * If the glyph is completely empty, null is returned.
     */
    public static int @Nullable [] findVerticalBorders(int width, int @NotNull [] glyph) {
        int left = -1;
        outer:
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < 16; y++) {
                if (((glyph[y] >>> (width - x - 1)) & 1) == 1) {
                    left = x;
                    break outer;
                }
            }
        }
        
        if (left == -1)
            return null;
        
        int right = -1;
        outer:
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < 16; y++) {
                if (((glyph[y] >>> (width - x - 1)) & 1) == 1) {
                    right = x;
                    break outer;
                }
            }
        }
        
        return new int[] {left, right};
    }
    
}