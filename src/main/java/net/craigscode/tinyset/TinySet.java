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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * TinySet is a wrapper of the JDBC. TinySet aims to simplify simple SQL operations in Java.
 * With only the JDBC, Exceptions must constantly be handled and retrieving data/setting up prepared statements is time-consuming.
 * Using the tools provided with TinySet, basic SQL operations become quick and painless.
 */
@SuppressWarnings("unused")
public class TinySet implements Iterable<TinySet> {

    private static final List<TinySet> commitCollection = new ArrayList<>();
    private static Connection connection;
    private final Incrementer in;
    private Incrementer out;
    private final PreparedStatement statement;
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
            throw new TinyException(e);
        } catch (NullPointerException e) {
            throw new TinyException("[TinySet] No connection has been established. Use connect(...), connectByFile(...), or connectByResource(...) with credentials.");
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
            throw new TinyException("[TinySet] Could not connect to database! Make sure you have configured the JDBC Driver.", e);
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
            throw new TinyException(e);
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
                set.statement.execute();
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException g) {
            System.out.println("[TinySet] There was an error committing data. Rolling back...");
            g.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException s) {
                throw new TinyException("[TinySet] There was a problem rolling back!", s);
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
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Gets the current Connection for any manual usage for operations outside the scope of TinySet or something TinySet has yet to implement.
     *
     * @return TinySet's connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Gets the current PreparedStatement for any manual usage for operations outside the scope of TinySet or something TinySet has yet to implement.
     *
     * @return This TinySet's instance of the PreparedStatement
     */
    public PreparedStatement getStatement() {
        return statement;
    }

    /**
     * Gets the current ResultSet for any manual usage for operations outside the scope of TinySet or something TinySet has yet to implement.
     *
     * @return This TinySet's instance of the ResultSet
     */
    public ResultSet getResultSet() {
        return rs;
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
            throw new TinyException(e);
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
            query();
            return rs.getBigDecimal(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link BigDecimal} values returned from the query
     */
    public List<BigDecimal> getBigDecList() {
        query();
        return stream().map(TinySet::getBigDec).collect(Collectors.toList());
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
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next boolean in the current row.
     *
     * @return The next boolean in the current row
     */
    public boolean getBoolean() {
        try {
            query();
            return rs.getBoolean(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Boolean} values returned from the query
     */
    public List<Boolean> getBooleanList() {
        query();
        return stream().map(TinySet::getBoolean).collect(Collectors.toList());
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
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next byte in the current row.
     *
     * @return The next byte in the current row
     */
    public byte getByte() {
        try {
            query();
            return rs.getByte(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Byte} values returned from the query
     */
    public List<Byte> getByteList() {
        query();
        return stream().map(TinySet::getByte).collect(Collectors.toList());
    }

    /**
     * Sets the next byte in the parameterized query.
     *
     * @param b Next byte for the parameterized query
     * @return The current TinySet
     */
    public TinySet setByte(byte b) {
        try {
            statement.setByte(in.get(), b);
            return this;
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next byte array in the current row.
     *
     * @return The next byte array in the current row
     */
    public byte[] getBytes() {
        try {
            query();
            return rs.getBytes(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Byte} array values returned from the query
     */
    public List<Byte[]> getBytesList() {
        query();
        List<byte[]> bytes = stream().map(TinySet::getBytes).collect(Collectors.toList());
        // The byte array needs to be converted from a primitive array to boxed array.
        return bytes.stream().map(b -> {
            Byte[] B = new Byte[b.length];
            for (int i = 0; i < b.length; i++) {
                B[i] = b[i];
            }
            return B;
        }).collect(Collectors.toList());
    }

    /**
     * Sets the next byte array in the parameterized query.
     *
     * @param b Next byte array for the parameterized query
     * @return The current TinySet
     */
    public TinySet setBytes(byte[] b) {
        try {
            statement.setBytes(in.get(), b);
            return this;
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next {@link LocalDate} in the current row.
     *
     * @return The next {@link LocalDate} in the current row
     */
    public LocalDate getDate() {
        try {
            query();
            Date date = rs.getDate(out.get());
            if (date == null) return null;
            else return date.toLocalDate();
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link LocalDate} values returned from the query
     */
    public List<LocalDate> getDateList() {
        query();
        return stream().map(TinySet::getDate).collect(Collectors.toList());
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
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next double in the current row.
     *
     * @return The next double in the current row
     */
    public double getDouble() {
        try {
            query();
            return rs.getDouble(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Double} values returned from the query
     */
    public List<Double> getDoubleList() {
        query();
        return stream().map(TinySet::getDouble).collect(Collectors.toList());
    }

    /**
     * Sets the next double in the parameterized query.
     *
     * @param d Next double for the parameterized query
     * @return The current TinySet
     */
    public TinySet setDouble(double d) {
        try {
            statement.setDouble(in.get(), d);
            return this;
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next float in the current row.
     *
     * @return The next float in the current row
     */
    public float getFloat() {
        try {
            query();
            return rs.getFloat(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Float} values returned from the query
     */
    public List<Float> getFloatList() {
        query();
        return stream().map(TinySet::getFloat).collect(Collectors.toList());
    }

    /**
     * Sets the next float in the parameterized query.
     *
     * @param f Next float for the parameterized query
     * @return The current TinySet
     */
    public TinySet setFloat(float f) {
        try {
            statement.setFloat(in.get(), f);
            return this;
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next int in the current row.
     *
     * @return The next int in the current row
     */
    public int getInt() {
        try {
            query();
            return rs.getInt(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Integer} values returned from the query
     */
    public List<Integer> getIntList() {
        query();
        return stream().map(TinySet::getInt).collect(Collectors.toList());
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
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next long in the current row.
     *
     * @return The next long in the current row
     */
    public long getLong() {
        try {
            query();
            return rs.getLong(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Long} values returned from the query
     */
    public List<Long> getLongList() {
        query();
        return stream().map(TinySet::getLong).collect(Collectors.toList());
    }

    /**
     * Sets the next long in the parameterized query.
     *
     * @param l Next long for the parameterized query
     * @return The current TinySet
     */
    public TinySet setLong(long l) {
        try {
            statement.setLong(in.get(), l);
            return this;
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next short in the current row.
     *
     * @return The next short in the current row
     */
    public short getShort() {
        try {
            query();
            return rs.getShort(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Short} values returned from the query
     */
    public List<Short> getShortList() {
        query();
        return stream().map(TinySet::getShort).collect(Collectors.toList());
    }

    /**
     * Sets the next short in the parameterized query.
     *
     * @param s Next short for the parameterized query
     * @return The current TinySet
     */
    public TinySet setShort(short s) {
        try {
            statement.setShort(in.get(), s);
            return this;
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next {@link String} in the current row.
     *
     * @return The next {@link String} in the current row
     */
    public String getString() {
        try {
            query();
            return rs.getString(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link String} values returned from the query
     */
    public List<String> getStringList() {
        query();
        return stream().map(TinySet::getString).collect(Collectors.toList());
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
            throw new TinyException(e);
        }
    }

    /**
     * Retrieves the next {@link Object} in the current row.
     *
     * @return The next {@link Object} in the current row
     */
    public Object getObject() {
        try {
            query();
            return rs.getObject(out.get());
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * @return A {@link List} populated with the {@link Object} values returned from the query
     */
    public List<Object> getObjectList() {
        query();
        return stream().map(TinySet::getObject).collect(Collectors.toList());
    }

    /**
     * Sets the next {@link Object} in the parameterized query.
     *
     * @param object Next {@link Object} for the parameterized query
     * @return The current TinySet
     */
    public TinySet setObject(Object object) {
        try {
            statement.setObject(in.get(), object);
            return this;
        } catch (SQLException e) {
            throw new TinyException(e);
        }
    }

    /**
     * Returns a Stream of a TinySet.
     *
     * @return Stream object of the current TinySet
     */
    public Stream<TinySet> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Executes the query that is prepared in {@link TinySet#statement} and sets the pointer to the first row in the ResultSet.
     */
    private void query() {
        if (rs == null) {
            try {
                rs = statement.executeQuery();
                rs.next();
            } catch (SQLException e) {
                throw new TinyException(e);
            }
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
