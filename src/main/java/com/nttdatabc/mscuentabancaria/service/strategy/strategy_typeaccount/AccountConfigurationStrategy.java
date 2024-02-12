package com.nttdatabc.mscuentabancaria.service.strategy.strategy_typeaccount;


import static com.nttdatabc.mscuentabancaria.utils.Constantes.*;

import com.google.gson.Gson;
import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.model.enums.TypeAccountBank;
import com.nttdatabc.mscuentabancaria.model.response.CustomerExt;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



/**
 * Configura la cuenta según la estrategia específica implementada.
 */
public interface AccountConfigurationStrategy {
  Mono<Void> configureAccount(Account account, CustomerExt customerExt);

  /**
   * Validate has credit.
   *
   * @param kafkaConsumerListener service.
   * @param customerId            id acccount.
   * @throws ErrorResponseException error.
   */
  default Mono<Void> validateHasCredit(KafkaConsumerListener kafkaConsumerListener, String customerId, KafkaTemplate<String, String> kafkaTemplate) {
    Map<String, String> request = new HashMap<>();
    request.put("customerId", customerId);
    Gson gson = new Gson();
    String customerIdJson = gson.toJson(request);
    kafkaTemplate.send("has-card-debt", customerIdJson);

    return kafkaConsumerListener.getCustomerHasCardDeb()
        .flatMap(s -> {
          if (s.contains("false")) {
            return Mono.error(new ErrorResponseException(REQUIRED_CREDIT_VIP,
                HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT));
          }
          return Mono.empty();
        });

  }

  /**
   * Validate required account corriente.
   *
   * @param accountList list.
   * @throws ErrorResponseException error.
   */
  default Mono<Void> validateHasCorriente(Flux<Account> accountList) {
    return accountList
        .any(account -> account.getTypeAccount().equalsIgnoreCase(TypeAccountBank.CORRIENTE.toString()))
        .flatMap(hasCorriente -> {
          if (!hasCorriente) {
            return Mono.error(new ErrorResponseException(REQUIRED_CUENTA_CORRIENTE, HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT));
          }
          return Mono.empty();
        })
        .switchIfEmpty(Mono.error(new ErrorResponseException(REQUIRED_CUENTA_CORRIENTE, HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT)))
        .then();
  }
}

