package edu.unl.exceptionamplifier.testcases;

import edu.unl.order.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class OrderManagementResourceTest {
    private OrderManagementResource createResource(double balance, int inventory) throws SQLException {
        OrderRepository repo = new OrderRepository();
        repo.initDatabase(balance);
        repo.addInventory("BOOK", inventory);
        ProductPriceService priceService = new MockProductPriceService();
        OrderManagementService service = new OrderManagementService(repo, priceService);
        return new OrderManagementResource(service);
    }

    @Test
    public void testPlaceAndCancelOrder() {
        try {
            OrderManagementResource resource = createResource(500.0, 5);
            int orderId = resource.placeOrder("BOOK", 2);
            assertEquals(400.0, resource.getBalance(), 0.01);
            assertEquals(3, resource.getInventory("BOOK"));

            resource.cancelOrder(orderId);
            assertEquals(500.0, resource.getBalance(), 0.01);
            assertEquals(5, resource.getInventory("BOOK"));
            assertEquals("CANCELLED", resource.getOrderStatus(orderId));
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testPlaceOrder_InsufficientBalance() throws SQLException {
        OrderManagementResource resource = createResource(20.0, 5);
        assertThrows(InsufficientBalanceException.class, () -> resource.placeOrder("BOOK", 1));
    }

    @Test
    public void testPlaceOrder_InsufficientInventory() throws SQLException {
        OrderManagementResource resource = createResource(500.0, 1);
        assertThrows(InventoryNotEnoughException.class, () -> resource.placeOrder("BOOK", 2));
    }

    @Test
    public void testPlaceOrder_HttpTimeout() throws SQLException {
        OrderManagementResource resource = createResource(500.0, 5);
        try {
            resource.placeOrder("BOOK_timeout", 1);
            fail("应当抛出异常");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("超时"));
        } catch (Exception e) {
            fail("应当抛出IOException: " + e.getMessage());
        }
    }

    @Test
    public void testPlaceOrder_Http502() throws SQLException {
        OrderManagementResource resource = createResource(500.0, 5);
        try {
            resource.placeOrder("BOOK_502", 1);
            fail("应当抛出异常");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("502"));
        } catch (Exception e) {
            fail("应当抛出IOException: " + e.getMessage());
        }
    }

    @Test
    public void testPlaceOrder_HttpDisconnect() throws SQLException {
        OrderManagementResource resource = createResource(500.0, 5);
        try {
            resource.placeOrder("BOOK_disconnect", 1);
            fail("应当抛出异常");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("断开"));
        } catch (Exception e) {
            fail("应当抛出IOException: " + e.getMessage());
        }
    }

    @Test
    public void testPlaceOrder_HttpSuccess() throws Exception {
        OrderManagementResource resource = createResource(500.0, 5);
        resource.placeOrder("BOOK", 1);
        assertEquals(450.0, resource.getBalance(), 0.01);
    }
}
