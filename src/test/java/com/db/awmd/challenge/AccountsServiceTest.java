package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountOverdraftException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Before
  public void clearUp() {
    this.accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }

  @Test
  public void transferBalance() throws Exception {
    Account accountFrom = createAccount("Id-123", new BigDecimal(3000));
    Account accountTo = createAccount("Id-456", new BigDecimal(1000));

    this.accountsService.transferBalance(accountFrom, accountTo, new BigDecimal(600) );

    assertThat(accountFrom.getBalance()).isEqualByComparingTo("2400");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("1600");
  }

  @Test
  public void transferBalanceOverDraft() throws Exception {
    Account accountFrom = createAccount("Id-123", new BigDecimal(3000));
    Account accountTo = createAccount("Id-456", new BigDecimal(1000));
    try {
      this.accountsService.transferBalance(accountFrom, accountTo, new BigDecimal(6000));
      fail("Overdraft is not allowed should fail");
    }catch (AccountOverdraftException aoe) {
      assertThat(aoe.getMessage()).isEqualTo("Account Id Id-123 has insufficient balance.");
    }
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("3000");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("1000");
  }

  private Account createAccount(String accountId, BigDecimal balance) throws Exception {
     Account account = new Account(accountId);
     account.setBalance(balance);
     this.accountsService.createAccount(account);
     return account;
  }
}
