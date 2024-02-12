package com.nttdatabc.mscuentabancaria.utils;

import static com.nttdatabc.mscuentabancaria.utils.Constantes.*;
import static com.nttdatabc.mscuentabancaria.utils.Utilitarios.convertStrToCustomerExt;

import com.google.gson.Gson;
import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.model.enums.TypeAccountBank;
import com.nttdatabc.mscuentabancaria.model.response.CustomerExt;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;



/**
 * Class.
 */
@Component
public class AccountValidator {

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;


  /**
   * Valida que los campos esenciales de la cuenta no sean nulos.
   *
   * @param account La cuenta que se va a validar.
   * @throws ErrorResponseException Si algún campo esencial es nulo.
   */
  public static Mono<Void> validateAccountsNoNulls(Account account) {
    return Mono.just(account)
        .filter(c -> c.getCustomerId() != null)
        .filter(c -> c.getCurrentBalance() != null)
        .filter(c -> c.getTypeAccount() != null)
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_ERROR_REQUEST,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  /**
   * Valida que los campos de la cuenta no estén vacíos.
   *
   * @param account La cuenta que se va a validar.
   * @throws ErrorResponseException Si algún campo esencial está vacío.
   */
  public static Mono<Void> validateAccountEmpty(Account account) {
    return Mono.just(account)
        .filter(c -> !c.getCustomerId().isEmpty())
        .filter(c -> !c.getCurrentBalance().toString().isBlank())
        .filter(c -> !c.getTypeAccount().isBlank())
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_VALUE_EMPTY,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  /**
   * Verifica que el tipo de cuenta sea válido.
   *
   * @param account La cuenta que se va a verificar.
   * @throws ErrorResponseException Si el tipo de cuenta no es válido.
   */
  public static Mono<Void> verifyTypeAccount(Account account) {
    return Mono.just(account)
        .filter(c -> {
          String typeAccount = c.getTypeAccount();
          return typeAccount.equalsIgnoreCase(TypeAccountBank.AHORRO.toString())
              || typeAccount.equalsIgnoreCase(TypeAccountBank.CORRIENTE.toString())
              || typeAccount.equalsIgnoreCase(TypeAccountBank.PLAZO_FIJO.toString())
              || typeAccount.equalsIgnoreCase(TypeAccountBank.VIP.toString())
              || typeAccount.equalsIgnoreCase(TypeAccountBank.PYME.toString());
        })
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_ERROR_TYPE_ACCOUNT,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  /**
   * Verifica que los valores de la cuenta sean válidos.
   *
   * @param account La cuenta que se va a verificar.
   * @throws ErrorResponseException Si los valores no son válidos.
   */
  public static Mono<Void> verifyValues(Account account) {
    return Mono.just(account)
        .filter(c -> c.getCurrentBalance().doubleValue() > VALUE_MIN_ACCOUNT_BANK)
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_ERROR_VALUE_MIN,
            HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST)))
        .then();
  }

  /**
   * Verifica la existencia de un cliente mediante su ID.
   *
   * @param customerId El ID del cliente.
   * @return La información del cliente si existe.
   * @throws ErrorResponseException Si el cliente no existe.
   */
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
            customerExt.set_id(customerId);
            return Mono.just(customerExt);
          } else {
            return Mono.error(new ErrorResponseException(EX_NOT_FOUND_RECURSO,
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND));
          }
        });
  }

  public Mono<Void> verifyCustomerDebCredit(String customerId, KafkaConsumerListener kafkaConsumerListener) {
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


}
