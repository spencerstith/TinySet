package net.craigscode.tinyset;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * TinySet is a wrapper of the JDBC. TinySet aims to simplify simple SQL operations in Java.
 * With only the JDBC, Exceptions must constantly be handled and retrieving data/setting up prepared statements is time-consuming.
 * Using the tools provided with TinySet, basic SQL operations become quick and painless.
 */
@SuppressWarnings("unused")
public class TinySet implements Iterable<TinySet> {

    private static final List<TinySet> commitCollection = new ArrayList<>();
    private static Connection connection;
    private Incrementer in, out;
    private PreparedStatement statement;
    private ResultSet rs;

    /**
     * Creates a new TinySet object.
     *
     * @param query Query that TinySet will contain
     */
    public TinySet(String query) {
        try {
            statement = connection.prepareStatement(query);
            in = new Incrementer();
            out = new Incrementer();
        } catch (SQLException e) {
            System.out.println("TinySet could not prepare the statement!");
            e.printStackTrace();
        }
    }

    /**
     * Establishes a static database connection for all TinySet objects to use.
     *
     * @param url      Database url
     * @param user     Database user
     * @param password Database password
     */
    public static void connect(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.err.println("Could not connect to database...");
            e.printStackTrace();
        }
    }

    /**
     * Establishes a static database connection for all TinySet objects to use.
     * This method allows TinySet to get database credentials from a Properties file.
     * <p>
     * Properties file must contain the fields <i>url</i>, <i>user</i>, <i>password</i>
     *
     * @param file path to Properties file
     */
    public static void connectByFile(String file) {
        try {
            InputStream stream = Files.newInputStream(Paths.get(file));
            Properties properties = new Properties();
            properties.load(stream);
            connect(properties.getProperty("url"), properties.getProperty("user"), properties.getProperty("password"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Establishes a static database connection for all TinySet objects to use.
     * This method allows TinySet to get database credentials from a Properties file
     * that is contained in the Class' resources path.
     * <p>
     * Properties file must contain the fields <i>url</i>, <i>user</i>, <i>password</i>
     *
     * @param resource path to Properties file within Class' resources path
     */
    public static void connectByResource(String resource) {
        try {
            InputStream stream = TinySet.class.getResourceAsStream("/" + resource);
            Properties properties = new Properties();
            properties.load(stream);
            connect(properties.getProperty("url"), properties.getProperty("user"), properties.getProperty("password"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the autoCommit property.
     * <br>
     * When autoCommit is true (default), {@link #commit()} must be called to execute the statement.
     * When autoCommit is false, no query is executed until either {@link #commit(TinySet...)} or {@link  #commitCollection()} is called.
     *
     * @param autoCommit whether TinySet should autoCommit
     */
    public static void setAutoCommit(boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            System.err.println("Could not set autocommit to " + autoCommit + "!");
            e.printStackTrace();
        }
    }

    /**
     * Adds a single TinySet object to the collection of TinySets to be committed on the next call to {@link #commitCollection()}.
     * This should only be used when autoCommit is false.
     *
     * @param set TinySet to be added to the collection of transactions to be committed.
     */
    public static void collect(TinySet set) {
        commitCollection.add(set);
    }

    /**
     * Commits all TinySet objects in the list of TinySet objects in {@param sets}.
     * If an SQLException is thrown while committing, all transactions are automatically rolled back.
     * This should only be used when autoCommit is false.
     *
     * @param sets List of TinySet objects to be committed.
     */
    public static void commit(TinySet... sets) {
        try {
            for (TinySet set : sets) {
                System.out.println(set.statement);
                set.statement.execute();
            }
            connection.commit();
        } catch (SQLException g) {
            System.out.println("There was an error committing data. Rolling back...");
            g.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException s) {
                System.out.println("Oh god there was a problem rolling back.");
                s.printStackTrace();
            }
        }
    }

    /**
     * Commits all TinySet objects in the TinySet collection.
     * If an SQLException is thrown while committing, all transactions are automatically rolled back.
     * This method clears the collection after committing.
     * This should only be used when autoCommit is false.
     */
    public static void commitCollection() {
        TinySet[] sets = new TinySet[commitCollection.size()];
        for (int i = 0; i < commitCollection.size(); i++) {
            sets[i] = commitCollection.get(i);
        }
        commit(sets);
        commitCollection.clear();
    }

    /**
     * Commits the statement contained in the TinySet.
     */
    public void commit() {
        try {
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            System.out.println("Could not execute command!");
            e.printStackTrace();
        }
    }

    /**
     * Checks whether the set has more information to offer.
     *
     * @return Whether the set has more rows from the query
     */
    public boolean next() {
        try {
            out = new Incrementer();
            if (rs == null) {
                rs = statement.executeQuery();
            }
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            throw JDBCRefusal.refusal("ResultSet's next value");
        }
    }

    /**
     * Skips over the next column in the row.
     */
    public void skip() {
        out.get();
    }

    /**
     * Retrieves the next {@link BigDecimal} in the current row.
     *
     * @return The next {@link BigDecimal} in the current row
     */
    public BigDecimal getBigDec() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getBigDecimal(out.get());
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("BigDecimal");
        }
    }

    /**
     * Sets the next {@link BigDecimal} in the parameterized query.
     *
     * @param decimal Next {@link BigDecimal} for the parameterized query
     * @return The current TinySet
     */
    public TinySet setBigDec(BigDecimal decimal) {
        try {
            statement.setBigDecimal(in.get(), decimal);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("BigDecimal");
        }
    }

    /**
     * Retrieves the next boolean in the current row.
     *
     * @return The next boolean in the current row
     */
    public boolean getBoolean() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getBoolean(out.get());
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("boolean");
        }
    }

    /**
     * Sets the next boolean in the parameterized query.
     *
     * @param b Next boolean for the parameterized query
     * @return The current TinySet
     */
    public TinySet setBoolean(boolean b) {
        try {
            statement.setBoolean(in.get(), b);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("boolean");
        }
    }

    /**
     * Retrieves the next {@link LocalDate} in the current row.
     *
     * @return The next {@link LocalDate} in the current row
     */
    public LocalDate getDate() {
        try {
            if (rs == null) {
                query();
            }
            Date date = rs.getDate(out.get());
            if (date == null) return null;
            else return date.toLocalDate();
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("Date");
        }
    }

    /**
     * Sets the next {@link LocalDate} in the parameterized query.
     *
     * @param date Next {@link LocalDate} for the parameterized query
     * @return The current TinySet
     */
    public TinySet setDate(LocalDate date) {
        try {
            statement.setDate(in.get(), Date.valueOf(date));
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("Date");
        }
    }

    /**
     * Retrieves the next int in the current row.
     *
     * @return The next int in the current row
     */
    public int getInt() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getInt(out.get());
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("Integer");
        }
    }

    /**
     * Sets the next int in the parameterized query.
     *
     * @param i Next int for the parameterized query
     * @return The current TinySet
     */
    public TinySet setInt(int i) {
        try {
            statement.setInt(in.get(), i);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("Integer");
        }
    }

    /**
     * Retrieves the next {@link String} in the current row.
     *
     * @return The next {@link String} in the current row
     */
    public String getString() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getString(out.get());
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("String");
        }
    }

    /**
     * Sets the next {@link String} in the parameterized query.
     *
     * @param string Next {@link String} for the parameterized query
     * @return The current TinySet
     */
    public TinySet setString(String string) {
        try {
            statement.setString(in.get(), string);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("String");
        }
    }

    /**
     * Executes the query that is prepared in {@link TinySet#statement} and sets the pointer to the first row in the ResultSet.
     */
    private void query() {
        try {
            rs = statement.executeQuery();
            rs.next();
        } catch (SQLException e) {
            System.out.println("Something went wrong when executing query!");
            e.printStackTrace();
        }
    }

    public Iterator<TinySet> iterator() {
        return new Iterator<>() {
            private boolean hasNext = false, didNext = false;

            public boolean hasNext() {
                if (!didNext) {
                    hasNext = TinySet.this.next();
                    didNext = true;
                }
                return hasNext;
            }

            public TinySet next() {
                if (!didNext) TinySet.this.next();
                didNext = false;
                return TinySet.this;
            }
        };
    }

    public String toString() {
        return statement.toString();
    }

}
