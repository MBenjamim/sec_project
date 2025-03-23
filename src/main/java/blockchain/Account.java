package main.java.blockchain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    @JsonIgnore
    private Address address;
    private Long balance;

    @JsonIgnore
    private Bytes code;
    @JsonIgnore
    private Map<UInt256, UInt256> storage;

    public Account(Address address, Long balance) {
        this.address = address;
        this.balance = balance;
        this.storage = new HashMap<>();
    }

    public String toJson() {
        String json = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            json = objectMapper.writeValueAsString(this);
            if (json != null) {
                objectMapper.readValue(json, Account.class);
            }
            return json;
        } catch (Exception e) {
            logger.error("Failed to convert account to JSON: {}", json, e);
            return null;
        }
    }

    public static Account fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Account.class);
        } catch (Exception e) {
            logger.error("Failed to convert JSON to account: {}", json, e);
            return null;
        }
    }
}
