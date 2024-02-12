package com.nttdatabc.mscuentabancaria.utils;

import static com.nttdatabc.mscuentabancaria.utils.Constantes.*;
import static com.nttdatabc.mscuentabancaria.utils.Utilitarios.convertStrToCustomerExt;

import com.google.gson.Gson;
import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.DebitCard;
import com.nttdatabc.mscuentabancaria.model.response.CustomerExt;
import com.nttdatabc.mscuentabancaria.repository.DebitCardRepository;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase debitCardValidator.
 */
@Component
public class DebitCardValidator {
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;
  public static Mono<Void> validateDebitCardNoNulls(DebitCard debitCard) {
    return Mono.just(debitCard)
        .filter(c -> c.getCustomerId() != null)
        .filter(c -> c.getAccountIdPrincipal() != null)
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_ERROR_REQUEST,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  public static Mono<Void> validateDebitCardUpdateNoNulls(DebitCard debitCard) {
    return Mono.just(debitCard)
        .filter(c -> c.get_id() != null)
        .filter(c -> c.getAccountsSecundary() != null)
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_ERROR_REQUEST,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  public static Mono<Void> validateDebitCardEmpty(DebitCard debitCard) {
    return Mono.just(debitCard)
        .filter(c -> !c.getCustomerId().isEmpty())
        .filter(c -> !c.getAccountIdPrincipal().isBlank())
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_VALUE_EMPTY,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  public static Mono<Void> validateDebitCardUpdateEmpty(DebitCard debitCard) {
    return Mono.just(debitCard)
        .filter(c -> !c.get_id().isEmpty())
        .filter(c -> !c.getAccountsSecundary().isEmpty())
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_VALUE_EMPTY,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  public Mono<CustomerExt> verifyCustomerExists(String customerId, KafkaConsumerListener kafkaConsumerListener) {
    Map<String, String> request = new HashMap<>();
    request.put("customerId", customerId);
    Gson gson = new Gson();
    String customerIdJson = gson.toJson(request);
    kafkaTemplate.send("verify-customer-exist", customerIdJson);

    return kafkaConsumerListener.getCustomerVerificationResponse()
        .flatMap(response -> {
          if (!response.contains("error")) {
            CustomerExt customerExt = convertStrToCustomerExt(response);
            return Mono.just(customerExt);
          } else {
            return Mono.error(new ErrorResponseException(EX_NOT_FOUND_RECURSO,
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND));
          }
        });
  }

  public  Mono<Void> verifyCustomerDebCredit(String customerId, KafkaConsumerListener kafkaConsumerListener) {
    Map<String, String> request = new HashMap<>();
    request.put("customerId", customerId);
    Gson gson = new Gson();
    String customerIdJson = gson.toJson(request);
    kafkaTemplate.send("has-debt-credit", customerIdJson);

    return kafkaConsumerListener.getCustomerHasDebtResponse()
        .flatMap(response -> {
          if (response.contains("true")) {
            return Mono.error(new ErrorResponseException(EX_ERROR_CUSTOMER_HAS_DEBT,
                HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT));
          }
          return Mono.empty();
        });
  }

  public static Mono<Void> verifyDuplicateNumberDebitCard(String debitCardId, DebitCardRepository debitCardRepository) {
    return Mono.defer(() -> {
      Mono<DebitCard> findDebitCard = debitCardRepository.findByNumberCard(debitCardId);
      return findDebitCard.hasElement()
          .flatMap(aBoolean -> {
            if (aBoolean) {
              return Mono.error(new ErrorResponseException(EX_ERROR_NUMBER_CARD_EXISTS, HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT));
            } else {
              return Mono.empty();
            }
          });
    }).then();
  }
}
