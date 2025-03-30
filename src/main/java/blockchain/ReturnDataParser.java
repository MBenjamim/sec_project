package main.java.blockchain;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import main.java.utils.DataUtils;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class ReturnDataParser {
    private static final Logger logger = LoggerFactory.getLogger(ReturnDataParser.class);

    public static Object getResult(ByteArrayOutputStream byteArrayOutputStream, ReturnType type) {
        boolean success = checkSuccess(byteArrayOutputStream);

        switch (type) {
            case UINT8:
            case UINT256:
                if (!success) return -1;
                return extractUInt256FromReturnData(byteArrayOutputStream).toLong();
            case BOOL:
                if (!success) return false;
                return !extractUInt256FromReturnData(byteArrayOutputStream).isZero();
            case STRING:
                if (!success) return null;
                return extractStringFromReturnData(byteArrayOutputStream);
        }
        return null;
    }

    public static JsonObject jsonFromTracer(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        // logger.debug(Arrays.toString(lines));
        return JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
    }

    /**
     * Gets from stack the memory location (offset) and the size of return data.
     * Use the offset and size to get data returned by EVM as hex string without prefix (0x)
     */
    public static String getReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        JsonObject instruction = jsonFromTracer(byteArrayOutputStream);

        JsonArray stack = instruction.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String memory = instruction.get("memory").getAsString();
        return memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
    }

    public static String getStringFromABI(String string) {
        int stringOffset = Integer.decode("0x"+string.substring(0, 32 * 2));
        int stringLength = Integer.decode("0x"+string.substring(stringOffset * 2, stringOffset * 2 + 32 * 2));
        String hexString = string.substring(stringOffset * 2 + 32 * 2, stringOffset * 2 + 32 * 2 + stringLength * 2);

        return new String(DataUtils.hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    }

    /**
     * Return true if smart contract execution was successful, false otherwise.
     */
    public static boolean checkSuccess(ByteArrayOutputStream byteArrayOutputStream) {
        JsonObject instruction = jsonFromTracer(byteArrayOutputStream);

        String opName = instruction.get("opName").getAsString();
        if (opName.equals("RETURN")) return true;
        if (opName.equals("REVERT")) return false;

        logger.error("Failure! UNKNOWN: {}", instruction);
        return false;
    }

    /**
     * UInt256 value that can be used as e.g. long, int, boolean, address.
     */
    public static UInt256 extractUInt256FromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String returnData = getReturnData(byteArrayOutputStream);
        return UInt256.fromHexString("0x"+returnData);
    }

    /**
     * String that corresponds to the returned String by the smart contract.
     */
    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String returnData = getReturnData(byteArrayOutputStream);
        return getStringFromABI(returnData);
    }

    /**
     * String that corresponds to the error message when the smart contract fails to execute,
     * if error message is not a String returns null.
     */
    public static String extractStringFromErrorMessage(ByteArrayOutputStream byteArrayOutputStream) {
        JsonObject instruction = jsonFromTracer(byteArrayOutputStream);
        try {
            String errorMessage = instruction.get("error").getAsString().substring(4 * 2); // ignore first 4 bytes
            return getStringFromABI(errorMessage);
        } catch (Exception e) {
            logger.error("Failed to extract error message from: {}", instruction);
            return null;
        }
    }
}
