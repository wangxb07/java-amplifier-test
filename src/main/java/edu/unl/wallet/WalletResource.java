package edu.unl.wallet;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 钱包资源入口，供测试调用
 */
public class WalletResource {
    private final WalletService service;

    public WalletResource(WalletService service) {
        this.service = service;
    }

    public WalletResource(double initialBalance) {
        WalletRepository repository = new WalletRepository();
        WalletService svc = new WalletService(repository);
        Map<String, Double> init = new HashMap<>();
        init.put("ETH", initialBalance);
        svc.init(init);
        this.service = svc;
    }

    public void deposit(String chain, double amount) throws SQLException {
        service.deposit(chain, amount);
    }

    public void transfer(String chain, String toAddress, double amount) throws SQLException, InsufficientBalanceException {
        service.transfer(chain, toAddress, amount);
    }

    public void crossChainSwap(String fromChain, String toChain, double amount) throws SQLException, InsufficientBalanceException {
        service.crossChainSwap(fromChain, toChain, amount);
    }

    public double getBalance(String chain) throws SQLException {
        return service.getBalance(chain);
    }
}
