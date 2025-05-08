package edu.unl.stock;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class StockTradingRepository {
    private static final String DB_URL = "jdbc:sqlite:stock_trading.db";

    public void initDatabase(double initialBalance) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS account (id INTEGER PRIMARY KEY, balance REAL)");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS portfolio (symbol TEXT PRIMARY KEY, quantity INTEGER)");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS trade (\n" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "symbol TEXT NOT NULL,\n" +
                        "quantity INTEGER NOT NULL,\n" +
                        "price REAL NOT NULL,\n" +
                        "type TEXT NOT NULL,\n" +
                        "trade_time DATETIME DEFAULT CURRENT_TIMESTAMP\n)");
            }
            // 若无余额记录，则插入初始余额
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM account")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO account (id, balance) VALUES (1, ?)");) {
                        ps.setDouble(1, initialBalance);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }

    public void updateBalance(double amount) throws IOException {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE account SET balance = balance + ? WHERE id=1")) {
                ps.setDouble(1, amount);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) {
            throw new IOException("数据库更新余额失败: " + e.getMessage(), e);
        }
    }

    public void updatePosition(String symbol, int delta) throws IOException {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(DB_URL)) {
            int oldQty = getPosition(symbol);
            if (oldQty == 0 && delta > 0) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO portfolio (symbol, quantity) VALUES (?, ?)")) {
                    ps.setString(1, symbol);
                    ps.setInt(2, delta);
                    ps.executeUpdate();
                }
            } else if (oldQty + delta == 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM portfolio WHERE symbol = ?")) {
                    ps.setString(1, symbol);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE portfolio SET quantity = quantity + ? WHERE symbol = ?")) {
                    ps.setInt(1, delta);
                    ps.setString(2, symbol);
                    ps.executeUpdate();
                }
            }
        } catch (java.sql.SQLException e) {
            throw new IOException("数据库更新持仓失败: " + e.getMessage(), e);
        }
    }

    public void insertTrade(String symbol, int quantity, double price, String type) throws IOException {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO trade (symbol, quantity, price, type) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, symbol);
                ps.setInt(2, quantity);
                ps.setDouble(3, price);
                ps.setString(4, type);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) {
            throw new IOException("数据库插入交易流水失败: " + e.getMessage(), e);
        }
    }

    public double getBalance() throws IOException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT balance FROM account WHERE id=1")) {
            if (rs.next()) {
                return rs.getDouble(1);
            } else {
                throw new IOException("未找到账户余额记录");
            }
        } catch (java.sql.SQLException e) {
            throw new IOException("数据库查询余额失败: " + e.getMessage(), e);
        }
    }

    public int getPosition(String symbol) throws IOException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM portfolio WHERE symbol=?")) {
            ps.setString(1, symbol);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        } catch (java.sql.SQLException e) {
            throw new IOException("数据库查询持仓失败: " + e.getMessage(), e);
        }
    }
}
