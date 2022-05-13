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

@SuppressWarnings("unused")
public class TinySet implements Iterable<TinySet> {

    private static final List<TinySet> commitCollection = new ArrayList<>();
    private static Connection connection;
    private Incrementer in, out;
    private PreparedStatement statement;
    private ResultSet rs;

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

    public static void connect(String url, String user, String pass) {
        try {
            connection = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            System.err.println("Could not connect to database...");
            e.printStackTrace();
        }
    }

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

    public static void setAutoCommit(boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            System.err.println("Could not set autocommit to " + autoCommit + "!");
            e.printStackTrace();
        }
    }

    public static void collect(TinySet set) {
        commitCollection.add(set);
    }

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

    public static void commitCollection() {
        TinySet[] sets = new TinySet[commitCollection.size()];
        for (int i = 0; i < commitCollection.size(); i++) {
            sets[i] = commitCollection.get(i);
        }
        commit(sets);
        commitCollection.clear();
    }

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

    public void skip() {
        out.get();
    }

    public BigDecimal bigDec() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getBigDecimal(out.get());
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("BigDecimal");
        }
    }

    public TinySet bigDec(BigDecimal decimal) {
        try {
            statement.setBigDecimal(in.get(), decimal);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("BigDecimal");
        }
    }

    public boolean bool() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getBoolean(out.get());
        } catch (SQLException e) {
            System.out.println("JDBC refuses to give you the boolean!");
            throw JDBCRefusal.refusal("boolean");
        }
    }

    public TinySet bool(boolean b) {
        try {
            statement.setBoolean(in.get(), b);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("boolean");
        }
    }

    public LocalDate date() {
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

    public TinySet date(LocalDate date) {
        try {
            statement.setDate(in.get(), Date.valueOf(date));
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("Date");
        }
    }

    public int integer() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getInt(out.get());
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("Integer");
        }
    }

    public TinySet integer(int i) {
        try {
            statement.setInt(in.get(), i);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("Integer");
        }
    }

    public String string() {
        try {
            if (rs == null) {
                query();
            }
            return rs.getString(out.get());
        } catch (SQLException e) {
            throw JDBCRefusal.refusal("String");
        }
    }

    public TinySet string(String string) {
        try {
            statement.setString(in.get(), string);
            return this;
        } catch (SQLException e) {
            throw JDBCRefusal.dislike("String");
        }
    }

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
