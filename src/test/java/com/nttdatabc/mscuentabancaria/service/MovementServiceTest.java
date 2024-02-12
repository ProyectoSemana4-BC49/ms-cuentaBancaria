package com.nttdatabc.mscuentabancaria.service;

import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.model.Movement;
import com.nttdatabc.mscuentabancaria.repository.MovementRepository;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class MovementServiceTest {

  @Mock
  private MovementRepository movementRepository;
  @Mock
  private AccountServiceImpl accountServiceImpl;
  @InjectMocks
  private MovementServiceImpl movementService;

  @BeforeEach
  public void setup(){
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void createMovementDepositService_Success() {
    // Arrange
    Movement movement = new Movement();
    movement.setAccountId("account1");
    movement.setTypeMovement("deposito");
    movement.setDestination("34324");
    movement.setMount(BigDecimal.valueOf(343));
    Account account = new Account();
    account.setTypeAccount("ahorro");
    account.setCustomerId("4324");
    account.setCurrentBalance(BigDecimal.valueOf(222));

    when(accountServiceImpl.getAccountByIdService(movement.getAccountId()))
        .thenReturn(Mono.just(account));
    when(movementRepository.save(movement))
        .thenReturn(Mono.just(movement));
    when(accountServiceImpl.updateAccountServide(any()))
        .thenReturn(Mono.empty());
    when(movementRepository.findByAccountId(movement.getAccountId())).thenReturn(Flux.empty());

    // Act
    Mono<Void> result = movementService.createMovementDepositService(movement);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(movementRepository).save(movement);
  }
  @Test
  void createMovementDepositService_Error() {
    // Arrange
    Movement movement = new Movement();
    movement.setAccountId("account1");
    movement.setTypeMovement("deposito");
    movement.setDestination("34324");
    movement.setMount(BigDecimal.valueOf(343));
    Account account = new Account();
    account.setTypeAccount("err");
    account.setCustomerId("4324");
    account.setCurrentBalance(BigDecimal.valueOf(222));

    when(accountServiceImpl.getAccountByIdService(movement.getAccountId()))
        .thenReturn(Mono.just(account));
    when(movementRepository.save(movement))
        .thenReturn(Mono.just(movement));
    when(accountServiceImpl.updateAccountServide(any()))
        .thenReturn(Mono.empty());
    when(movementRepository.findByAccountId(movement.getAccountId())).thenReturn(Flux.empty());

    // Act
    Mono<Void> result = movementService.createMovementDepositService(movement);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);

    // Verify that movementRepository.save() is called with the movement
    verify(movementRepository,never()).save(movement);
  }
}
