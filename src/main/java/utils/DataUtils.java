package main.java.utils;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Utility class for data conversion operations.
 */
public final class DataUtils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return the hexadecimal string representation of the byte array
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts an integer to a byte array.
     *
     * @param value the integer value to convert
     * @return the byte array representation of the integer
     */
    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    /**
     * Converts a long to a byte array.
     *
     * @param value the long value to convert
     * @return the byte array representation of the long
     */
    public static byte[] longToBytes(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    public static String stringToHex(String input) {
        StringBuilder hexString = new StringBuilder();
        for (char c : input.toCharArray()) {
            hexString.append(String.format("%02x", (int) c));
        }
        return hexString.toString();
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            int value = Integer.parseInt(hexString.substring(i, i + 2), 16);
            byteArray[i / 2] = (byte) value;
        }

        return byteArray;
    }

    public static String getFunctionSignature(String functionName) {
        byte[] functionBytes = Numeric.hexStringToByteArray(stringToHex(functionName));
        String functionHash = Numeric.toHexStringNoPrefix(Hash.sha3(functionBytes));
        return functionHash.substring(0, 8); // first 4 bytes of the hash
    }

    public static String padHexString(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }

    public static String convertNumberToHex256Bit(long number) {
        BigInteger bigInt = BigInteger.valueOf(number);

        return String.format("%064x", bigInt);
    }
}
