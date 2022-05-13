package net.craigscode.tinyset;

public class Incrementer {

    private int i;

    public Incrementer() {
        i = 1;
    }

    public int get() {
        return i++;
    }

    public String toString() {
        return String.valueOf(i);
    }
}
