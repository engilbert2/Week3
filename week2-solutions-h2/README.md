# Week 2: Banking System with H2 Database

This project enhances Week 1's banking system by replacing file-based storage with H2 database while maintaining the exact same code structure.

## Changes from Week 1 🔄

1. File Storage to H2 Database
   - Replaced transactions.txt with H2 in-memory database
   - Added H2 dependency in pom.xml
   - Added database schema (schema.sql)
   - Added database configuration (application.properties)

2. New Database Package
   ```
   db/
   └── DatabaseConnection.java    # H2 connection management
   ```

3. Updated TransactionLogger
   - Same interface as Week 1:
     ```java
     public void logTransaction(String accountId, BigDecimal amount)
     public List<String> readTransactionHistory()
     public List<String> readTransactionHistory(String accountId)
     ```
   - Implementation changed from file I/O to H2 database
   - Same output format for transaction history

4. Everything Else Unchanged
   - Same package structure
   - Same service layer (AccountService)
   - Same model classes
   - Same exception handling
   - Same BankingSystem facade
   - Same Main class demo

## Project Structure 📁

```
week2-solutions-h2/
├── src/
│   ├── main/
│   │   ├── java/com/banking/
│   │   │   ├── model/           # Same as Week 1
│   │   │   │   ├── Account.java
│   │   │   │   ├── AccountType.java
│   │   │   │   ├── AccountFactory.java
│   │   │   │   ├── SavingsAccount.java
│   │   │   │   └── CheckingAccount.java
│   │   │   ├── service/         # Same as Week 1
│   │   │   │   └── AccountService.java
│   │   │   ├── util/            # Updated for H2
│   │   │   │   ├── TransactionLogger.java
│   │   │   │   └── StringProcessor.java
│   │   │   ├── exception/       # Same as Week 1
│   │   │   │   ├── BankingException.java
│   │   │   │   ├── AccountNotFoundException.java
│   │   │   │   └── InsufficientFundsException.java
│   │   │   ├── db/             # New in Week 2
│   │   │   │   └── DatabaseConnection.java
│   │   │   ├── BankingSystem.java  # Same as Week 1
│   │   │   └── Main.java           # Same as Week 1
│   │   └── resources/
│   │       ├── schema.sql           # New in Week 2
│   │       └── application.properties
└── pom.xml                          # Added H2 dependency
```

## Key Benefits of H2 🎯

1. Zero Setup Required
   - No database server installation needed
   - No database creation/configuration
   - Everything runs in-memory automatically

2. Same Code Structure
   - No changes to business logic
   - No changes to service layer API
   - Only storage implementation changed

3. Better Data Management
   - ACID transactions support
   - Proper rollback on errors
   - SQL-based querying
   - Data integrity through foreign keys

## Requirements

- Java 11 or higher
- Maven 3.6+

## Building and Running

1. Build the project:
   ```bash
   mvn clean compile
   ```

2. Run the application:
   ```bash
   mvn exec:java -Dexec.mainClass="com.banking.Main"
   ```

## Sample Output

The application demonstrates the same functionality as Week 1:
```
Creating accounts...
Created savings account: SavingsAccount[number=SAV001 balance=1000.00 interestRate=2.50%]
Created checking account: CheckingAccount[number=CHK001 balance=500.00 transactions=0]

Performing transactions...
Deposited $500 to savings
Savings balance: $1500.00
Transferred $300 from savings to checking
Savings balance: $1200.00
Checking balance: $800.00

Transaction History for SAV001:
2025-02-16 15:30:25.301244,SAV001,-300.00
2025-02-16 15:30:25.292461,SAV001,500.00
2025-02-16 15:30:25.283002,SAV001,1000.00
```

## Dependencies

- H2 Database (with MySQL compatibility mode)
- JUnit 4 (for testing)

## Next Steps

Future enhancements could include:
- Connection pooling
- More complex queries
- Additional transaction types
- Performance optimizations
