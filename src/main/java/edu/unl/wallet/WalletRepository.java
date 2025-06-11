package edu.unl.wallet;

import java.sql.*;
import java.util.Map;
import java.util.function.Function;

/**
 * 钱包仓库类，负责持久化存储和事务处理
 */
public class WalletRepository {
    private static final String DB_URL = "jdbc:sqlite:web3_wallet.db";

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

    public void initDatabase(Map<String, Double> initialBalances) {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS account (chain TEXT PRIMARY KEY, balance REAL)");
            }
            try (Statement cleanStmt = conn.createStatement()) {
                cleanStmt.executeUpdate("DELETE FROM account;");
            }
            for (Map.Entry<String, Double> entry : initialBalances.entrySet()) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO account (chain, balance) VALUES (?, ?)")) {
                    ps.setString(1, entry.getKey());
                    ps.setDouble(2, entry.getValue());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }

    public double getBalance(String chain) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT balance FROM account WHERE chain=?")) {
            ps.setString(1, chain);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                } else {
                    return 0.0;
                }
            }
        }
    }

    public void updateBalance(String chain, double delta) throws SQLException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO account (chain, balance) VALUES (?, COALESCE((SELECT balance FROM account WHERE chain=?),0)+?)"
                    + " ON CONFLICT(chain) DO UPDATE SET balance = balance + excluded.balance")) {
                ps.setString(1, chain);
                ps.setString(2, chain);
                ps.setDouble(3, delta);
                ps.executeUpdate();
            }
        }
    }

    public void crossChainSwap(String fromChain, String toChain, double amount) throws SQLException, InsufficientBalanceException {
        runInTransaction(conn -> {
            try {
                double fromBalance = getBalanceWithConnection(conn, fromChain);
                if (fromBalance < amount) {
                    throw new InsufficientBalanceException("余额不足: " + fromBalance);
                }
                updateBalanceWithConnection(conn, fromChain, -amount);
                updateBalanceWithConnection(conn, toChain, amount);
                return true;
            } catch (SQLException | InsufficientBalanceException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private double getBalanceWithConnection(Connection conn, String chain) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM account WHERE chain=?")) {
            ps.setString(1, chain);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                } else {
                    return 0.0;
                }
            }
        }
    }

    private void updateBalanceWithConnection(Connection conn, String chain, double delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO account (chain, balance) VALUES (?, COALESCE((SELECT balance FROM account WHERE chain=?),0)+?)"
                        + " ON CONFLICT(chain) DO UPDATE SET balance = balance + excluded.balance")) {
            ps.setString(1, chain);
            ps.setString(2, chain);
            ps.setDouble(3, delta);
            ps.executeUpdate();
        }
    }
}
