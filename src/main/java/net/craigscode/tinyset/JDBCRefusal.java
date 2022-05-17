package net.craigscode.tinyset;

import java.sql.SQLException;

class JDBCRefusal extends RuntimeException {

    JDBCRefusal(String message) {
        super(message);
    }

    /**
     * This method is called when there is a problem retrieving information from the database.
     * If the error indicates the incorrect datatype being requested is inconsistent with what the database has,
     * handle that here with extra information. Otherwise, send it to be interpreted by {@link JDBCRefusal#interpret(SQLException)}
     *
     * @param type     Incorrect type that is trying to be pulled from database
     * @param instance TinySet instance to get further information from
     * @param e        SQL exception to get further information from
     * @return Elaboration about incorrect data type matching
     */
    public static JDBCRefusal refusal(String type, TinySet instance, SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.equals("22018")) {
            try {
                int col = instance.out().previous();
                String t = instance.rs().getMetaData().getColumnTypeName(col);
                return new JDBCRefusal(String.format("[TinySet] Column %d is not a %s. Database says it is a %s.", col, type, getName(t)));
            } catch (SQLException q) {
                q.printStackTrace();
                return new JDBCRefusal("");
            }
        } else {
            return interpret(e);
        }
    }

    /**
     * This method tries to make more sense of the default error message from SQLExceptions.
     * There are a lot, so this method will be ever-growing.
     * There are also a lot of proprietary codes out there and that will prevent the growth of this method as well.
     * The SQL states are found and referenced from https://en.wikipedia.org/wiki/SQLSTATE .
     *
     * @param e SQLException to be interpreted
     * @return Reason why JDBC threw an SQLException
     */
    public static JDBCRefusal interpret(SQLException e) {
        String state = e.getSQLState();
        System.out.println(state);
        if (state == null) {
            e.printStackTrace();
            return new JDBCRefusal("[TinySet] Could not parse error message. Original SQLException is shown.");
        } else if (state.startsWith("21")) {
            return new JDBCRefusal("[TinySet] Column count doesn't match value count!");
        } else if (state.equals("42000")) {
            // Using a substring because the verbose SQL message is long.
            return new JDBCRefusal("[TinySet] There is an error in your SQL syntax at " + e.getMessage().substring(134));
        } else if (state.equals("42S02")) {
            return new JDBCRefusal("[TinySet] " + e.getMessage());
        } else if (state.equals("HY000")) {
            return new JDBCRefusal("[TinySet] Could not be certain about error message. Likely cause: incorrect type match in INSERT or UPDATE. Original message: " + e.getMessage());
        } else {
            e.printStackTrace();
            return new JDBCRefusal("[TinySet] Could not parse error message. Original message: " + e.getMessage());
        }
    }

    public static String getName(String type) {
        return switch (type) {
            case "INTEGER", "INT", "TINYINT", "SMALLINT" -> "int";
            case "FLOAT" -> "float";
            case "DOUBLE" -> "double";
            case "DECIMAL" -> "BigDecimal";
            case "VARCHAR" -> "String";
            case "BOOLEAN" -> "boolean";
            case "DATE" -> "Date";
            case "JAVA_OBJECT" -> "Object";
            default -> type;
        };
    }

}
