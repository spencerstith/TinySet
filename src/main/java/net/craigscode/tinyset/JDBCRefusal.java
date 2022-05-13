package net.craigscode.tinyset;

class JDBCRefusal extends RuntimeException {

    JDBCRefusal(String message) {
        super(message);
    }

    public static JDBCRefusal dislike(String c) {
        return new JDBCRefusal("JDBC didn't like that " + c + "!");
    }

    public static JDBCRefusal refusal(String c) {
        return new JDBCRefusal("JDBC refuses to give you that " + c);
    }

}
