package com.nttdatabc.mscuentabancaria.service;

import com.nttdatabc.mscuentabancaria.model.DebitCard;
import com.nttdatabc.mscuentabancaria.repository.DebitCardRepository;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@SpringBootTest
public class DebitCardServiceTest {
  @Mock
  private DebitCardRepository debitCardRepository;
  @InjectMocks
  private DebitCardServiceImpl debitCardService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }


  @Test
  void getDebitCardByIdService_Success() {
    // Arrange
    String debitCardId = "debitCard1";
    DebitCard debitCard = new DebitCard();
    debitCard.set_id(debitCardId);

    when(debitCardRepository.findById(debitCardId))
        .thenReturn(Mono.just(debitCard));

    // Act
    Mono<DebitCard> result = debitCardService.getDebitCardByIdService(debitCardId);

    // Assert
    StepVerifier.create(result)
        .expectNext(debitCard)
        .verifyComplete();

    verify(debitCardRepository).findById(debitCardId);
  }
  @Test
  void getDebitCardByIdService_Error() {
    // Arrange
    String debitCardId = "debitCard1";
    DebitCard debitCard = new DebitCard();
    debitCard.set_id(debitCardId);

    when(debitCardRepository.findById(debitCardId))
        .thenReturn(Mono.empty());
    // Act
    Mono<DebitCard> result = debitCardService.getDebitCardByIdService(debitCardId);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);

    verify(debitCardRepository).findById(debitCardId);
  }

  @Test
  void deleteDebitCardService_Success() {
    // Arrange
    String debitCardId = "debitCard1";
    DebitCard debitCard = new DebitCard();
    debitCard.set_id(debitCardId);

    when(debitCardRepository.delete(debitCard))
        .thenReturn(Mono.empty());

    when(debitCardRepository.findById(debitCardId))
        .thenReturn(Mono.just(debitCard));

    // Act
    Mono<Void> result = debitCardService.deleteDebitCardService(debitCardId);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(debitCardRepository).delete(debitCard);
  }

  @Test
  void deleteDebitCardService_Error() {
    // Arrange
    String debitCardId = "debitCard1";

    when(debitCardRepository.findById(debitCardId))
        .thenReturn(Mono.empty());

    // Act
    Mono<Void> result = debitCardService.deleteDebitCardService(debitCardId);

    // Assert
    StepVerifier.create(result)
        .expectError(ErrorResponseException.class);

    verify(debitCardRepository, never()).delete(any());
  }
}
