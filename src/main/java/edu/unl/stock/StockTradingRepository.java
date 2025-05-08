package edu.unl.stock;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

/**
 * 股票交易数据库操作仓库
 * 支持事务操作和原子性保证
 */
public class StockTradingRepository {
    private static final String DB_URL = "jdbc:sqlite:stock_trading.db";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    /**
     * 在事务中执行数据库操作
     * @param action 要执行的操作，接收一个Connection并返回结果
     * @param <T> 返回结果类型
     * @return 操作结果
     * @throws IOException 如果数据库操作失败
     */
    public <T> T runInTransaction(Function<Connection, T> action) throws IOException {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // 开启事务
            
            T result = action.apply(conn); // 执行操作
            
            conn.commit(); // 提交事务
            return result;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // 发生异常时回滚
                } catch (SQLException ex) {
                    throw new IOException("事务回滚失败: " + ex.getMessage(), ex);
                }
            }
            throw new IOException("事务执行失败: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit); // 恢复原始设置
                    conn.close();
                } catch (SQLException e) {
                    // 记录关闭连接失败，但不抛出异常
                    System.err.println("关闭数据库连接失败: " + e.getMessage());
                }
            }
        }
    }

    public void initDatabase(double initialBalance) {
        try (Connection conn = getConnection()) {
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
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE account SET balance = balance + ? WHERE id=1")) {
                ps.setDouble(1, amount);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException("数据库更新余额失败: " + e.getMessage(), e);
        }
    }

    public void updatePosition(String symbol, int delta) throws IOException, SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        try (Connection conn = getConnection()) {
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
        } catch (SQLException e) {
            throw new IOException("数据库更新持仓失败: " + e.getMessage(), e);
        }
    }

    public void insertTrade(String symbol, int quantity, double price, String type) throws SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException, IOException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO trade (symbol, quantity, price, type) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, symbol);
                ps.setInt(2, quantity);
                ps.setDouble(3, price);
                ps.setString(4, type);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException("数据库插入交易流水失败: " + e.getMessage(), e);
        }
    }

    public double getBalance() throws IOException, SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT balance FROM account WHERE id=1")) {
            if (rs.next()) {
                return rs.getDouble(1);
            } else {
                throw new IOException("未找到账户余额记录");
            }
        } catch (SQLException e) {
            throw new IOException("数据库查询余额失败: " + e.getMessage(), e);
        }
    }

    public int getPosition(String symbol) throws IOException, SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM portfolio WHERE symbol=?")) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        } catch (SQLException e) {
            throw new IOException("数据库查询持仓失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行交易操作（原子性保证）
     * 包含余额变更、持仓变更和交易流水记录，确保全部成功或全部失败
     * 
     * @param symbol 股票代码
     * @param quantity 数量（买入为正，卖出为负）
     * @param price 价格
     * @param type 交易类型（"buy" 或 "sell"）
     * @throws IOException 如果数据库操作失败
     */
    public void executeTradeTransaction(String symbol, int quantity, double price, String type) throws IOException, SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        runInTransaction(conn -> {
            try {
                // 计算金额
                double amount = quantity * price * ("buy".equals(type) ? -1 : 1);
                
                // 1. 更新余额
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account SET balance = balance + ? WHERE id=1")) {
                    ps.setDouble(1, amount);
                    ps.executeUpdate();
                }
                
                // 2. 更新持仓
                int positionDelta = "buy".equals(type) ? quantity : -quantity;
                int oldQty;
try {
    oldQty = getPositionWithConnection(conn, symbol);
} catch (java.util.concurrent.TimeoutException | InsufficientBalanceException e) {
    throw new RuntimeException(e);
}
                
                if (oldQty == 0 && positionDelta > 0) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO portfolio (symbol, quantity) VALUES (?, ?)")) {
                        ps.setString(1, symbol);
                        ps.setInt(2, positionDelta);
                        ps.executeUpdate();
                    }
                } else if (oldQty + positionDelta == 0) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM portfolio WHERE symbol = ?")) {
                        ps.setString(1, symbol);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE portfolio SET quantity = quantity + ? WHERE symbol = ?")) {
                        ps.setInt(1, positionDelta);
                        ps.setString(2, symbol);
                        ps.executeUpdate();
                    }
                }
                
                // 3. 插入交易流水
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO trade (symbol, quantity, price, type) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, symbol);
                    ps.setInt(2, Math.abs(quantity));
                    ps.setDouble(3, price);
                    ps.setString(4, type);
                    ps.executeUpdate();
                }
                
                return true; // 操作成功
            } catch (SQLException e) {
                throw new RuntimeException("交易执行失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 使用已有连接查询持仓（事务内部使用）
     */
    private int getPositionWithConnection(Connection conn, String symbol) throws SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM portfolio WHERE symbol=?")) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        }
    }
}
