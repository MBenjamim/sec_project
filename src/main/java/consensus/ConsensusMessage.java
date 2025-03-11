package main.java.consensus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import main.java.common.Message;
import main.java.common.MessageType;


/**
 * Represents a consensus message in the blockchain network.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusMessage extends Message  {

    private long consensusIdx;
    private int epochTS;

    /**
     * Constructor for the ConsensusMessage class.
     *
     * @param id           the unique identifier for the message
     * @param type         the type of the message
     * @param sender       the unique identifier for the sender
     * @param consensusIdx the consensus instance identifier
     * @param epochTS      the epoch timestamp in the corresponding consensus instance
     */
    public ConsensusMessage(long id, MessageType type, int sender, long consensusIdx, int epochTS) {
        super(id, type, sender);
        this.consensusIdx = consensusIdx;
        this.epochTS = epochTS;
    }

    /**
     * Constructor for the Message class with content.
     *
     * @param id           the unique identifier for the message
     * @param type         the type of the message
     * @param sender       the unique identifier for the sender
     * @param content      the content of the message
     * @param consensusIdx the consensus instance identifier
     * @param epochTS      the epoch timestamp in the corresponding consensus instance
     */
    public ConsensusMessage(long id, MessageType type, int sender, String content, long consensusIdx, int epochTS) {
        super(id, type, sender, content);
        this.consensusIdx = consensusIdx;
        this.epochTS = epochTS;
    }

    /**
     * Retrieves the properties of the message to be signed.
     *
     * @return a string representation of the properties to be signed
     */
    @JsonIgnore
    public String getPropertiesToSign() {
        return super.getPropertiesToSign() + consensusIdx + epochTS;
    }
}
