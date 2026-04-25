package dev.blitical.jigsawDB.util;

public class JSONClass {
    public String first;
    public String second;
    public int third;
    public long fourth;
    public boolean fifth;
    public Chained chained;

    public static class Chained {
        public String first;
        public int second;
        public boolean third;
    }
}
