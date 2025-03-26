package main.java.blockchain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.MutableAccount;
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
    private Wei balance;
    @JsonIgnore
    private Bytes code = null;
    @JsonIgnore
    private Map<UInt256, UInt256> storage = null;

    public Account(MutableAccount account, String type) {
        this.balance = account.getBalance();
        if (type.equals("contract")) {
            this.code = account.getCode();
            this.storage = account.getUpdatedStorage();
        }
    }

    @JsonProperty("balance")
    public Long getBalanceJson() {
        return balance.toLong();
    }

    @JsonProperty("balance")
    public void setBalanceJson(Long balance) {
        this.balance = Wei.of(balance);
    }

    @JsonProperty("code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCodeJson() {
        return code.toHexString();
    }

    @JsonProperty("code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public void setCodeJson(String code) {
        this.code = Bytes.fromHexString(code);
    }

    @JsonProperty("storage")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getStorageJson() {
        Map<String, String> storageMap = new HashMap<>();
        for (Map.Entry<UInt256, UInt256> entry : storage.entrySet()) {
            storageMap.put(entry.getKey().toHexString(), entry.getValue().toHexString());
        }
        return storageMap;
    }

    @JsonProperty("storage")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public void setStorageJson(Map<String, String> storage) {
        Map<UInt256, UInt256> newStorage = new HashMap<>();
        for (Map.Entry<String, String> entry : storage.entrySet()) {
            newStorage.put(UInt256.fromHexString(entry.getKey()), UInt256.fromHexString(entry.getValue()));
        }
        this.storage = newStorage;
    }
}
