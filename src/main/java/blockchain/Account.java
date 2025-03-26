package main.java.blockchain;

import com.fasterxml.jackson.annotation.JsonCreator;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class Account {
    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    private static final int DECIMALS = 2;

    @JsonIgnore
    private AccountType type;
    @JsonIgnore
    private Wei balance;
    @JsonIgnore
    private Bytes code = null;
    @JsonIgnore
    private Map<UInt256, UInt256> storage = null;

    @JsonCreator
    public Account() {
        // by default (when code or storage are not null turns to contract account)
        this.type = AccountType.EOA;
    }

    public Account(MutableAccount account) {
        this.balance = account.getBalance();
        this.type = (account.getCode().equals(Bytes.fromHexString(""))) ? AccountType.EOA : AccountType.CONTRACT;
        if (AccountType.CONTRACT.equals(type)) {
            this.code = account.getCode();
            this.storage = account.getUpdatedStorage();
        }
    }

    @JsonProperty("balance")
    public String getBalanceJson() {
        return BigDecimal.valueOf(balance.toLong()).movePointLeft(DECIMALS).toPlainString();
    }

    @JsonProperty("balance")
    public void setBalanceJson(String balanceString) {
        BigDecimal balance = new BigDecimal(balanceString);
        this.balance = Wei.of(balance.movePointRight(DECIMALS).longValue());
    }

    @JsonProperty("code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCodeJson() {
        return (AccountType.EOA.equals(type)) ? null : code.toHexString();
    }

    @JsonProperty("code")
    public void setCodeJson(String code) {
        if (code == null) {
            this.code = null;
            this.type = AccountType.EOA;
        } else {
            this.code = Bytes.fromHexString(code);
            this.type = AccountType.CONTRACT;
        }
    }

    @JsonProperty("storage")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getStorageJson() {
        if (AccountType.EOA.equals(type)) return null;

        Map<String, String> storageMap = new HashMap<>();
        for (Map.Entry<UInt256, UInt256> entry : storage.entrySet()) {
            storageMap.put(entry.getKey().toHexString(), entry.getValue().toHexString());
        }
        return storageMap;
    }

    @JsonProperty("storage")
    public void setStorageJson(Map<String, String> storage) {
        if (storage == null) {
            this.storage = null;
            this.type = AccountType.EOA;
        } else {
            Map<UInt256, UInt256> newStorage = new HashMap<>();
            for (Map.Entry<String, String> entry : storage.entrySet()) {
                newStorage.put(UInt256.fromHexString(entry.getKey()), UInt256.fromHexString(entry.getValue()));
            }
            this.storage = newStorage;
            this.type = AccountType.CONTRACT;
        }
    }
}
