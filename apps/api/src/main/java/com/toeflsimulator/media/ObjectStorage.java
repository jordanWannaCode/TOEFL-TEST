package com.toeflsimulator.media;
public interface ObjectStorage {
    void put(String key, byte[] data);
    byte[] get(String key);
}
