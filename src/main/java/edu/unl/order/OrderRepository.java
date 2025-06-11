package edu.unl.order;

import java.sql.*;
import java.util.function.Function;

/**
 * 订单数据库操作仓库，提供事务保障。
 */
public class OrderRepository {
    private static final String DB_URL = "jdbc:sqlite:commerce_order.db";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private <T> T runInTransaction(Function<Connection, T> action) throws SQLException {
        Connection conn = null;
        boolean autoCommit = true;
        try {
            conn = getConnection();
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            T result = action.apply(conn);
            conn.commit();
            return result;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new SQLException("回滚事务失败: " + ex.getMessage(), ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                } catch (SQLException e) {
                    throw new SQLException("关闭数据库连接失败: " + e.getMessage(), e);
                }
            }
        }
    }

    public void initDatabase(double initialBalance) {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS account (id INTEGER PRIMARY KEY, balance REAL)");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS inventory (product TEXT PRIMARY KEY, quantity INTEGER)");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS orders (\n" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "product TEXT NOT NULL,\n" +
                        "quantity INTEGER NOT NULL,\n" +
                        "price REAL NOT NULL,\n" +
                        "status TEXT NOT NULL,\n" +
                        "order_time DATETIME DEFAULT CURRENT_TIMESTAMP\n)");
            }
            try (Statement cleanStmt = conn.createStatement()) {
                cleanStmt.executeUpdate("DELETE FROM inventory;");
                cleanStmt.executeUpdate("DELETE FROM orders;");
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO account (id, balance) VALUES (1, ?);")) {
                ps.setDouble(1, initialBalance);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }

    public void addInventory(String product, int quantity) throws SQLException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO inventory (product, quantity) VALUES (?, COALESCE((SELECT quantity FROM inventory WHERE product=?),0)+?)")) {
                ps.setString(1, product);
                ps.setString(2, product);
                ps.setInt(3, quantity);
                ps.executeUpdate();
            }
        }
    }

    public int getInventory(String product) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM inventory WHERE product=?")) {
            ps.setString(1, product);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        }
    }

    public double getBalance() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT balance FROM account WHERE id=1")) {
            if (rs.next()) {
                return rs.getDouble(1);
            } else {
                throw new SQLException("未找到账户余额记录");
            }
        }
    }

    private int insertOrder(Connection conn, String product, int quantity, double price, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO orders (product, quantity, price, status) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product);
            ps.setInt(2, quantity);
            ps.setDouble(3, price);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public int placeOrder(final String product, final int quantity, final double price) throws SQLException {
        return runInTransaction(conn -> {
            try {
                int currentInventory = getInventoryWithConnection(conn, product);
                if (currentInventory < quantity) {
                    throw new RuntimeException("库存不足");
                }

                double amount = quantity * price;
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account SET balance = balance - ? WHERE id=1")) {
                    ps.setDouble(1, amount);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("UPDATE inventory SET quantity = quantity - ? WHERE product=?")) {
                    ps.setInt(1, quantity);
                    ps.setString(2, product);
                    ps.executeUpdate();
                }

                return insertOrder(conn, product, quantity, price, "PLACED");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void cancelOrder(final int orderId) throws SQLException {
        runInTransaction(conn -> {
            try {
                Order order = getOrderWithConnection(conn, orderId);
                if (order == null) {
                    throw new RuntimeException("订单不存在");
                }
                if (!"PLACED".equals(order.getStatus())) {
                    throw new RuntimeException("订单状态无法取消");
                }

                double amount = order.getQuantity() * order.getPrice();
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account SET balance = balance + ? WHERE id=1")) {
                    ps.setDouble(1, amount);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE inventory SET quantity = quantity + ? WHERE product=?")) {
                    ps.setInt(1, order.getQuantity());
                    ps.setString(2, order.getProductId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status='CANCELLED' WHERE id=?")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }
                return true;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String getOrderStatus(int orderId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM orders WHERE id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                } else {
                    return null;
                }
            }
        }
    }

    private int getInventoryWithConnection(Connection conn, String product) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM inventory WHERE product=?")) {
            ps.setString(1, product);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        }
    }

    private Order getOrderWithConnection(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, product, quantity, price, status FROM orders WHERE id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Order(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getDouble(4), rs.getString(5));
                } else {
                    return null;
                }
            }
        }
    }
}
