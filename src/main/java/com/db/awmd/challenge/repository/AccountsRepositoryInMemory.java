package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountOverdraftException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

import java.math.BigDecimal;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
        "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) {
    return accounts.get(accountId);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }

  @Override
  public void transferBalance(Account accountFrom, Account accountTo, BigDecimal amount) throws AccountOverdraftException {
    doTransfer(accountFrom, accountTo, amount);
  }

  private void doTransfer(Account accountFrom, Account accountTo, BigDecimal amount) {
    checkBalance(accountFrom, amount);
    accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
    accountTo.setBalance(accountTo.getBalance().add(amount));
  }

  private void checkBalance(Account account, BigDecimal amount) throws AccountOverdraftException {
    //overdraft is not allowed, balance available in the account is less than the amount being transferred.
    if(account.getBalance().compareTo(amount) < 0  ) {
      throw new AccountOverdraftException("Account Id " + account.getAccountId() + " has insufficient balance.");
    }
  }
}
