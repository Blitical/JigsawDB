package dev.blitical.jigsawDB.util;

import java.io.Serializable;

public class SerializableClass implements Serializable {
    public String first;
    public String second;
    public int third;
    public long fourth;
    public boolean fifth;
    public Chained chained;

    public static class Chained implements Serializable {
        public String first;
        public int second;
        public boolean third;
    }
}
