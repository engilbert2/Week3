package com.banking.service;

import com.banking.BankingSystem;
import com.banking.model.*;
import com.banking.exception.*;
import org.junit.Before;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class AccountServiceTest {
    private BankingSystem bankingSystem;
    private AccountService accountService;
    private Account savingsAccount;
    private Account checkingAccount;

    @Before
    public void setUp() {
        bankingSystem = new BankingSystem();
        accountService = new AccountService(bankingSystem);

        // Create test accounts
        savingsAccount = AccountFactory.createAccount(
            AccountType.SAVINGS,
            "SAV001",
            new BigDecimal("1000.00")
        );

        checkingAccount = AccountFactory.createAccount(
            AccountType.CHECKING,
            "CHK001",
            new BigDecimal("500.00")
        );

        bankingSystem.addAccount(savingsAccount);
        bankingSystem.addAccount(checkingAccount);
    }

    @Test
    public void testSuccessfulTransfer() throws InsufficientFundsException {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("200.00");
        BigDecimal expectedSavingsBalance = new BigDecimal("800.00");
        BigDecimal expectedCheckingBalance = new BigDecimal("700.00");

        // Act
        accountService.transfer("SAV001", "CHK001", transferAmount);

        // Assert
        assertEquals(expectedSavingsBalance, savingsAccount.getBalance());
        assertEquals(expectedCheckingBalance, checkingAccount.getBalance());
    }

    @Test(expected = InsufficientFundsException.class)
    public void testTransferWithInsufficientFunds() throws InsufficientFundsException {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("2000.00");

        // Act
        accountService.transfer("SAV001", "CHK001", transferAmount);
        // Should throw InsufficientFundsException
    }

    @Test(expected = AccountNotFoundException.class)
    public void testTransferWithInvalidAccount() throws InsufficientFundsException {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("100.00");

        // Act
        accountService.transfer("INVALID", "CHK001", transferAmount);
        // Should throw AccountNotFoundException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransferWithNegativeAmount() throws InsufficientFundsException {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("-100.00");

        // Act
        accountService.transfer("SAV001", "CHK001", transferAmount);
        // Should throw IllegalArgumentException
    }
}
