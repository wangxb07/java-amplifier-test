package edu.unl.wallet;

import java.sql.SQLException;
import java.util.Map;

/**
 * 钱包业务逻辑
 */
public class WalletService {
    private final WalletRepository repository;

    public WalletService(WalletRepository repository) {
        this.repository = repository;
    }

    public void init(Map<String, Double> initialBalances) {
        repository.initDatabase(initialBalances);
    }

    /** 收款 */
    public void deposit(String chain, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        repository.updateBalance(chain, amount);
    }

    /** 转账 */
    public void transfer(String chain, String toAddress, double amount) throws SQLException, InsufficientBalanceException {
        if (amount <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        double balance = repository.getBalance(chain);
        if (balance < amount) {
            throw new InsufficientBalanceException("余额不足: " + balance);
        }
        // 这里只模拟扣除余额，忽略链上转账细节
        repository.updateBalance(chain, -amount);
    }

    /** 跨链兑换 */
    public void crossChainSwap(String fromChain, String toChain, double amount) throws SQLException, InsufficientBalanceException {
        if (amount <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        repository.crossChainSwap(fromChain, toChain, amount);
    }

    /**
     * 兑换其他虚拟币，包含手续费和网络检查
     */
    public void exchangeToken(String fromChain, String toChain, double amount, double fee, String network)
            throws SQLException, InsufficientBalanceException, RemoteApiException {
        if (amount <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (fee < 0) {
            throw new IllegalArgumentException("手续费不能为负数");
        }
        repository.exchangeToken(fromChain, toChain, amount, fee, network);
    }

    public double getBalance(String chain) throws SQLException {
        return repository.getBalance(chain);
    }
}
