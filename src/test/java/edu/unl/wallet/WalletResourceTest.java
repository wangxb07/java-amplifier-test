package edu.unl.wallet;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import edu.unl.wallet.RemoteApiException;

import static org.junit.jupiter.api.Assertions.*;

public class WalletResourceTest {

    private WalletResource createResource(double ethBalance) {
        WalletRepository repo = new WalletRepository();
        WalletService service = new WalletService(repo);
        Map<String, Double> init = new HashMap<>();
        init.put("ETH", ethBalance);
        service.init(init);
        return new WalletResource(service);
    }

    @Test
    public void testDepositTransferAndSwap() throws Exception {
        WalletResource resource = createResource(100.0);
        resource.deposit("ETH", 50.0);
        assertEquals(150.0, resource.getBalance("ETH"), 0.01);

        resource.transfer("ETH", "0xabc", 40.0);
        assertEquals(110.0, resource.getBalance("ETH"), 0.01);

        resource.crossChainSwap("ETH", "BNB", 10.0);
        assertEquals(100.0, resource.getBalance("ETH"), 0.01);
        assertEquals(10.0, resource.getBalance("BNB"), 0.01);

        resource.exchangeToken("ETH", "SOL", 20.0, 1.0, "mainnet");
        assertEquals(79.0, resource.getBalance("ETH"), 0.01);
        assertEquals(20.0, resource.getBalance("SOL"), 0.01);
    }

    @Test
    public void testTransferInsufficientBalance() throws SQLException {
        WalletResource resource = createResource(20.0);
        assertThrows(InsufficientBalanceException.class, () -> resource.transfer("ETH", "0xdef", 30.0));
    }

    @Test
    public void testExchangeInvalidNetwork() throws Exception {
        WalletResource resource = createResource(50.0);
        assertThrows(RemoteApiException.class, () -> resource.exchangeToken("ETH", "BNB", 10.0, 1.0, ""));
    }
}
