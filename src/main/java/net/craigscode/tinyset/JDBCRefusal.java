package net.craigscode.tinyset;

class JDBCRefusal extends RuntimeException {

    // TODO better error handling and type checking

    JDBCRefusal(String message) {
        super(message);
    }

    public static JDBCRefusal dislike(String s) {
        return new JDBCRefusal("TinySet cannot accept that " + s + "!");
    }

    public static JDBCRefusal refusal(String s) {
        return new JDBCRefusal("TinySet cannot give you that " + s);
    }

}
