package net.craigscode.tinyset;

import java.sql.SQLException;

/**
 * TinyException is used to catch SQLExceptions.
 * This allows for TinySet to do SQL operations while not forcing the user to use try/catch blocks for every SQL operation.
 * This allows for flexibility — if a user wishes to catch an exception, they can use:
 * <pre>
 *     try {
 *         ...
 *     } catch (TinyException e) {
 *         ...
 *     }
 * </pre>
 * However, if the user wishes only to print the stack trace of the SQLException, they need not do anything, since TinyException does that on its own.
 * Since TinyException is only used to catch SQLExceptions, they can be used in their place safely.
 */
@SuppressWarnings("unused")
class TinyException extends RuntimeException {

    public SQLException exception;

    TinyException(SQLException exception) {
        super(exception.getMessage());
        this.exception = exception;
    }

    TinyException(String m) {
        super(m);
    }

    TinyException(String m, SQLException exception) {
        super(m);
        this.exception = exception;
        System.err.println("[TinySet] Original error message:");
        exception.printStackTrace();
    }

}
