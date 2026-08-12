package com.snorklingturtle.photographer.util;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ByteArrayCompression {
    public static byte[] compress(byte[] input) {
        // Create a Deflater object
        Deflater deflater = new Deflater();
        deflater.setInput(input);

        // Create an output buffer
        byte[] output = new byte[input.length];

        // Perform compression
        deflater.finish();
        int compressedSize = deflater.deflate(output);
        deflater.end();

        // Copy only the compressed data to a new array
        byte[] compressedData = new byte[compressedSize];
        System.arraycopy(output, 0, compressedData, 0, compressedSize);

        return compressedData;
    }

    public static byte[] decompress(byte[] input) throws DataFormatException {
        // Create a Inflater object
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        // Create an output buffer
        byte[] output = new byte[128*128];

        // Perform decompression
        int decompressedSize = inflater.inflate(output);
        inflater.end();

        // Copy only the decompressed data to a new array
        byte[] decompressedData = new byte[decompressedSize];
        System.arraycopy(output, 0, decompressedData, 0, decompressedSize);

        return decompressedData;
    }
}
