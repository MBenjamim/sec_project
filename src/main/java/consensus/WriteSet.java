package main.java.consensus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WriteSet {
    private long timestamp;       
    private String value;       
}
