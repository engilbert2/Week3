package com.banking.service;
import java.util.HashMap;
import java.util.Map;

import com.banking.db.DatabaseConnection;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.BankingException;
import com.banking.exception.InsufficientFundsException;
import com.banking.model.Account;
import com.banking.model.AccountFactory;
import com.banking.model.AccountType;
import com.banking.util.TransactionLogger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AccountService {
    private final DatabaseConnection dbConnection;
    private final TransactionLogger transactionLogger;

    public AccountService() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.transactionLogger = new TransactionLogger();
    }

    public Account createAccount(AccountType type, String accountId, BigDecimal initialBalance) throws BankingException {
        Account account = AccountFactory.createAccount(type, accountId, initialBalance);
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO accounts (account_id, account_type, balance) VALUES (?, ?, ?)"
             )) {
            stmt.setString(1, accountId);
            stmt.setString(2, type.toString());
            stmt.setBigDecimal(3, initialBalance);
            stmt.executeUpdate();

            // Log initial deposit
            transactionLogger.logTransaction(accountId, initialBalance);
            return account;
        } catch (SQLException e) {
            throw new BankingException("Failed to create account: " + e.getMessage());
        }
    }

    public void deposit(String accountId, BigDecimal amount) throws BankingException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be positive");
        }

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update account balance
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance + ? WHERE account_id = ?"
                )) {
                    stmt.setBigDecimal(1, amount);
                    stmt.setString(2, accountId);
                    int updated = stmt.executeUpdate();
                    if (updated == 0) {
                        throw new AccountNotFoundException("Account not found: " + accountId);
                    }
                }

                // Log transaction
                transactionLogger.logTransaction(accountId, amount);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankingException("Failed to process deposit: " + e.getMessage());
        }
    }

    public void withdraw(String accountId, BigDecimal amount) throws BankingException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be positive");
        }

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check balance
                BigDecimal currentBalance = getBalance(accountId);
                if (currentBalance.compareTo(amount) < 0) {
                    throw new InsufficientFundsException("Insufficient funds for withdrawal", currentBalance, amount);
                }

                // Update account balance
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE account_id = ?"
                )) {
                    stmt.setBigDecimal(1, amount);
                    stmt.setString(2, accountId);
                    stmt.executeUpdate();
                }

                // Log transaction
                transactionLogger.logTransaction(accountId, amount.negate());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankingException("Failed to process withdrawal: " + e.getMessage());
        }
    }

    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount) throws BankingException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be positive");
        }

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check balance
                BigDecimal fromBalance = getBalance(fromAccountId);
                if (fromBalance.compareTo(amount) < 0) {
                    throw new InsufficientFundsException("Insufficient funds for transfer", fromBalance, amount);
                }

                // Update both accounts
                try (PreparedStatement withdrawStmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE account_id = ?"
                )) {
                    withdrawStmt.setBigDecimal(1, amount);
                    withdrawStmt.setString(2, fromAccountId);
                    withdrawStmt.executeUpdate();
                }

                try (PreparedStatement depositStmt = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance + ? WHERE account_id = ?"
                )) {
                    depositStmt.setBigDecimal(1, amount);
                    depositStmt.setString(2, toAccountId);
                    depositStmt.executeUpdate();
                }

                // Log transactions
                transactionLogger.logTransaction(fromAccountId, amount.negate());
                transactionLogger.logTransaction(toAccountId, amount);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankingException("Failed to process transfer: " + e.getMessage());
        }
    }

    public BigDecimal getBalance(String accountId) throws BankingException {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT balance FROM accounts WHERE account_id = ?"
             )) {
            stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal balance = rs.getBigDecimal("balance");
                    return balance != null ? balance : BigDecimal.ZERO;  // Ensure balance is never null
                }
                throw new AccountNotFoundException("Account not found: " + accountId);
            }
        } catch (SQLException e) {
            throw new BankingException("Failed to get balance: " + e.getMessage());
        }
    }


    public List<String> getTransactionHistory(String accountId) {
        return transactionLogger.readTransactionHistory(accountId);
    }

    public List<String> getAllTransactions() {
        return transactionLogger.readTransactionHistory();
    }

    public void deleteAccount(String accountId) throws BankingException {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // First delete related transactions (due to foreign key)
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM transactions WHERE account_id = ?"
                )) {
                    stmt.setString(1, accountId);
                    stmt.executeUpdate();
                }

                // Then delete the account
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM accounts WHERE account_id = ?"
                )) {
                    stmt.setString(1, accountId);
                    int updated = stmt.executeUpdate();
                    if (updated == 0) {
                        throw new AccountNotFoundException("Account not found: " + accountId);
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new BankingException("Failed to delete account: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BankingException("Database error while deleting account: " + e.getMessage());
        }
    } // <-- This curly brace properly closes deleteAccount()

    // ✅ Make sure these methods are OUTSIDE deleteAccount()
    public Map<String, Object> getAccountSummary() {
        Map<String, Object> summary = new HashMap<>();
        try (Connection conn = dbConnection.getConnection()) {
            // Count total accounts
            String sql = "SELECT COUNT(*) as total FROM accounts";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.put("totalAccounts", rs.getInt("total"));
                }
            }

            // Get total balance
            sql = "SELECT SUM(balance) as total FROM accounts";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    summary.put("totalBalance", total != null ? total : BigDecimal.ZERO);
                }
            }
        } catch (SQLException e) {
            throw new BankingException("Could not create summary", e);
        }
        return summary; // ✅ No error here anymore
    }


    // Add this method for Daily Transactions Report
        public Map<String, Object> getDailyTransactions() {
            Map<String, Object> report = new HashMap<>();

            try (Connection conn = dbConnection.getConnection()) {
                // Get today's deposits
                String sql =
                        "SELECT SUM(amount) as total " +
                                "FROM transactions " +
                                "WHERE DATE(transaction_date) = CURRENT_DATE " +
                                "AND amount > 0";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        BigDecimal total = rs.getBigDecimal("total");
                        report.put("totalDeposits", total != null ? total : BigDecimal.ZERO);
                    }
                }

                // Get today's withdrawals
                sql =
                        "SELECT SUM(amount) as total " +
                                "FROM transactions " +
                                "WHERE DATE(transaction_date) = CURRENT_DATE " +
                                "AND amount < 0";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        BigDecimal total = rs.getBigDecimal("total");
                        report.put("totalWithdrawals", total != null ? total : BigDecimal.ZERO);
                    }
                }
            } catch (SQLException e) {
                throw new BankingException("Could not create daily report", e);
            }

            return report;
        }

        // Add this method for Account Activity Report
        public Map<String, Object> getAccountActivity() {
            Map<String, Object> report = new HashMap<>();

            try (Connection conn = dbConnection.getConnection()) {
                // Find most active account
                String sql =
                        "SELECT account_id, COUNT(*) as tx_count " +
                                "FROM transactions " +
                                "GROUP BY account_id " +
                                "ORDER BY tx_count DESC " +
                                "LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        report.put("mostActiveAccount", rs.getString("account_id"));
                        report.put("transactionCount", rs.getInt("tx_count"));
                    }
                }

                // Find highest balance account
                sql =
                        "SELECT account_id, balance " +
                                "FROM accounts " +
                                "ORDER BY balance DESC " +
                                "LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        report.put("highestBalanceAccount", rs.getString("account_id"));
                        report.put("highestBalance", rs.getBigDecimal("balance"));
                    }
                }
            } catch (SQLException e) {
                throw new BankingException("Could not create activity report", e);
            }

            return report;
        }
    }

