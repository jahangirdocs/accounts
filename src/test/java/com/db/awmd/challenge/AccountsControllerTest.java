package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;

import com.db.awmd.challenge.service.NotificationService;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Mock
  private NotificationService notificationService;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  public void transferBalanceSuccessful() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountFrom\":\"Id-123\", \"accountTo\":\"Id-456\", \"amount\":300}")
            ).andExpect(status().isOk());

    Account accountFrom = accountsService.getAccount("Id-123");
    Account accountTo = accountsService.getAccount("Id-456");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("700");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("2300");

    notificationService.notifyAboutTransfer(accountFrom, String.format("amount [%s] has been successfully transferred to account Id [%s].", "300", "Id-456"));
    notificationService.notifyAboutTransfer(accountTo, String.format("amount [%s] has been deposited from account Id [%s].", "300", "Id-123"));
  }

  @Test
  public void transferBalanceInSeries() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-123\", \"accountTo\":\"Id-456\", \"amount\":300}")
    ).andExpect(status().isOk());

    Account account1 = accountsService.getAccount("Id-123");
    Account account2 = accountsService.getAccount("Id-456");
    assertThat(account1.getBalance()).isEqualByComparingTo("700");
    assertThat(account2.getBalance()).isEqualByComparingTo("2300");

    notificationService.notifyAboutTransfer(account1, String.format("amount [%s] has been successfully transferred to account Id [%s].", "300", "Id-456"));
    notificationService.notifyAboutTransfer(account2, String.format("amount [%s] has been deposited from account Id [%s].", "300", "Id-123"));

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-456\", \"accountTo\":\"Id-789\", \"amount\":400}")
    ).andExpect(status().isOk());


    account2 = accountsService.getAccount("Id-456");
    Account account3 = accountsService.getAccount("Id-789");
    assertThat(account2.getBalance()).isEqualByComparingTo("1900");
    assertThat(account3.getBalance()).isEqualByComparingTo("4400");

    notificationService.notifyAboutTransfer(account2, String.format("amount [%s] has been successfully transferred to account Id [%s].", "400", "Id-789"));
    notificationService.notifyAboutTransfer(account3, String.format("amount [%s] has been deposited from account Id [%s].", "400", "Id-456"));

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-789\", \"accountTo\":\"Id-123\", \"amount\":500}")
    ).andExpect(status().isOk());

    account1 = accountsService.getAccount("Id-123");
    account2 = accountsService.getAccount("Id-456");
    account3 = accountsService.getAccount("Id-789");
    assertThat(account1.getBalance()).isEqualByComparingTo("1200");
    assertThat(account2.getBalance()).isEqualByComparingTo("1900");
    assertThat(account3.getBalance()).isEqualByComparingTo("3900");

    notificationService.notifyAboutTransfer(account3, String.format("amount [%s] has been successfully transferred to account Id [%s].", "500", "Id-123"));
    notificationService.notifyAboutTransfer(account1, String.format("amount [%s] has been deposited from account Id [%s].", "500", "Id-789"));

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-789\", \"accountTo\":\"Id-456\", \"amount\":600}")
    ).andExpect(status().isOk());

    account1 = accountsService.getAccount("Id-123");
    account2 = accountsService.getAccount("Id-456");
    account3 = accountsService.getAccount("Id-789");
    assertThat(account1.getBalance()).isEqualByComparingTo("1200");
    assertThat(account2.getBalance()).isEqualByComparingTo("2500");
    assertThat(account3.getBalance()).isEqualByComparingTo("3300");

    notificationService.notifyAboutTransfer(account3, String.format("amount [%s] has been successfully transferred to account Id [%s].", "600", "Id-456"));
    notificationService.notifyAboutTransfer(account2, String.format("amount [%s] has been deposited from account Id [%s].", "600", "Id-789"));
  }

  @Test
  public void transferBalanceAccountFromEmpty() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"\", \"accountTo\":\"Id-456\", \"amount\":300}")
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void transferBalanceAccountToEmpty() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-123\", \"accountTo\":\"\", \"amount\":300}")
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void transferBalanceNoAccountFrom() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountTo\":\"Id-456\", \"amount\":300}")
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void transferBalanceNoAccountTo() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-456\", \"amount\":300}")
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void transferBalanceNoAmount() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-123\", \"accountTo\":\"Id-456\"}")
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void transferBalanceNoBody() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void transferBalanceAccountFromNotExists() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-888\", \"accountTo\":\"Id-456\", \"amount\":300}")
    ).andExpect(status().isBadRequest())
            .andExpect(content().string("Account Id Id-888 does not exists."));
  }

  @Test
  public void transferBalanceAccountToNotExists() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-123\", \"accountTo\":\"Id-777\", \"amount\":300}")
    ).andExpect(status().isBadRequest())
     .andExpect(content().string("Account Id Id-777 does not exists."));
  }

  @Test
  public void transferBalanceNegativeAmount() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-123\", \"accountTo\":\"Id-456\", \"amount\":-400}")
    ).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("Id-123");
    Account accountTo = accountsService.getAccount("Id-456");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("1000");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("2000");
  }

  @Test
  public void transferBalanceOverDraft() throws Exception {
    prepareAccounts();

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFrom\":\"Id-123\", \"accountTo\":\"Id-456\", \"amount\":2500.99}")
    ).andExpect(status().isBadRequest())
            .andExpect(content().string("Account Id Id-123 has insufficient balance."));

    Account accountFrom = accountsService.getAccount("Id-123");
    Account accountTo = accountsService.getAccount("Id-456");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("1000");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("2000");
  }


  private void prepareAccounts() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":1000}"))
            .andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-456\",\"balance\":2000}"))
            .andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-789\",\"balance\":4000}"))
            .andExpect(status().isCreated());
  }



  /*private String buildTransferRequest(String fromId, String toId, BigDecimal amount) {
    return "{"
            + fromId !=null ? "\"accountFrom\":" + "\"" + fromId + "\"" : ""
            + toId !=null ?   ",\"accountTo\":" + "\"" + toId + "\"" : ""
            + amount !=null ? ",\"amount\":" + amount : ""
            + "}";
  }*/
}
