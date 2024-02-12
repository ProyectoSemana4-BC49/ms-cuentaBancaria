package com.nttdatabc.mscuentabancaria.service;



import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.repository.AccountRepository;
import com.nttdatabc.mscuentabancaria.service.interfaces.AccountService;
import com.nttdatabc.mscuentabancaria.utils.AccountValidator;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.nttdatabc.mscuentabancaria.utils.Constantes.EX_NOT_FOUND_RECURSO;
import static org.mockito.Mockito.*;


@SpringBootTest
public class AccountServiceTest {

  @Mock
  private AccountRepository accountRepository;
  @Mock
  private AccountValidator accountValidator;
  @Mock
  private KafkaConsumerListener kafkaConsumerListener;
  @InjectMocks
  private AccountServiceImpl accountService;
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void getAccountById_Success() {
    // Arrange
    String accountId = "account1";
    Account account = new Account();
    account.setId(accountId);

    when(accountRepository.findById(accountId))
        .thenReturn(Mono.just(account));

    // Act
    Mono<Account> result = accountService.getAccountByIdService(accountId);

    // Assert
    StepVerifier.create(result)
        .expectNext(account)
        .verifyComplete();

    verify(accountRepository).findById(accountId);
  }
  @Test
  public void getAccountById_Not_Found() {
    // Arrange
    String accountId = "account1";
    Account account = new Account();
    account.setId(accountId);

    when(accountRepository.findById(accountId))
        .thenReturn(Mono.empty());

    // Act
    Mono<Account> result = accountService.getAccountByIdService(accountId);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);
    verify(accountRepository).findById(accountId);
  }

  @Test
  void getAccountsByCustomerIdService_ShouldReturnAccounts_WhenCustomerExists() {
    // Arrange
    String customerId = "customer1";
    Account account1 = new Account();
    account1.setCustomerId(customerId);
    Account account2 = new Account();
    account2.setCustomerId(customerId);

    when(accountValidator.verifyCustomerExists(customerId, kafkaConsumerListener))
        .thenReturn(Mono.empty());
    when(accountRepository.findByCustomerId(customerId))
        .thenReturn(Flux.just(account1, account2));

    // Act
    Flux<Account> result = accountService.getAccountsByCustomerIdService(customerId);

    // Assert
    StepVerifier.create(result)
        .expectNext(account1)
        .expectNext(account2)
        .verifyComplete();

    verify(accountValidator).verifyCustomerExists(customerId, kafkaConsumerListener);

    verify(accountRepository).findByCustomerId(customerId);
  }
  @Test
  void getAccountsByCustomerIdService_ShouldReturnError_WhenCustomerDoesNotExist() {
    // Arrange
    String customerId = "customer1";

    when(accountValidator.verifyCustomerExists(customerId, kafkaConsumerListener))
        .thenReturn(Mono.error(new ErrorResponseException(EX_NOT_FOUND_RECURSO,
            HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)));

    // Act
    Flux<Account> result = accountService.getAccountsByCustomerIdService(customerId);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class)
        .verify();

    verify(accountValidator).verifyCustomerExists(customerId, kafkaConsumerListener);
  }

  @Test
  void deleteAccountByIdService_ShouldDeleteAccount_WhenAccountExists() {
    // Arrange
    String accountId = "account1";
    Account account = new Account();
    account.setId(accountId);

    when(accountRepository.findById(accountId))
        .thenReturn(Mono.just(account));
    when(accountRepository.delete(account))
        .thenReturn(Mono.empty());

    // Act
    Mono<Void> result = accountService.deleteAccountByIdService(accountId);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(accountRepository).findById(accountId);
    verify(accountRepository).delete(account);
  }
  @Test
  void deleteAccountByIdService_ShouldReturnError_WhenAccountDoesNotExist() {
    // Arrange
    String accountId = "account1";

    when(accountRepository.findById(accountId))
        .thenReturn(Mono.empty());

    // Act
    Mono<Void> result = accountService.deleteAccountByIdService(accountId);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);
    verify(accountRepository).findById(accountId);
    verify(accountRepository, never()).delete(any());
  }


}
