package net.craigscode.tinyset;

/**
 * Incrementer is a helper class for TinySet.
 * The sole purpose of this class is to keep track of column numbers when retrieving information from a query.
 */
class Incrementer {

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
