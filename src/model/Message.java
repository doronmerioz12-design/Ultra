package model;

import java.time.LocalDateTime;


public class Message {
    private final int id;
    private final String rawCiphertext;
    private final String originFrequency;
    private final LocalDateTime interceptedAt;
    private final String cipherType;

    public Message(int id, String rawCiphertext, String originFrequency,String cipherType) {
        this.id = id;
        this.rawCiphertext = rawCiphertext;
        this.originFrequency = originFrequency;
        this.interceptedAt = LocalDateTime.now();
        this.cipherType = cipherType;
    }

    public int getId() {
        return id;
    }

    public String getRawCiphertext() {
        return rawCiphertext;
    }

    public String  getOriginFrequency() {
        return originFrequency;
    }
    public LocalDateTime getInterceptedAt() {
        return interceptedAt;
    }
    public String getCipherType() {
        return cipherType;
    }


}
