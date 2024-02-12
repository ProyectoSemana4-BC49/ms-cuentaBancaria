package com.nttdatabc.mscuentabancaria.service.strategy.strategy_typeaccount;

import static com.nttdatabc.mscuentabancaria.utils.Constantes.*;

import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.model.enums.TypeCustomer;
import com.nttdatabc.mscuentabancaria.model.response.CustomerExt;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;



/**
 * Configuration for Accounts Vip.
 */
public class VipAccountConfigurationStrategy implements AccountConfigurationStrategy {
  @Override
  public Mono<Void> configureAccount(Account account, CustomerExt customerExt)  {
    if (customerExt.getType().equalsIgnoreCase(TypeCustomer.EMPRESA.toString())) {
      return Mono.error(new ErrorResponseException(EMPRESA_NOT_PERMITTED_VIP,
          HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT));
    }

    if (account.getCurrentBalance().doubleValue() < MOUNT_MIN_OPEN_VIP) {
      return Mono.error(new ErrorResponseException(MOUNT_INSUFICIENT_CREATE_VIP,
          HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT));
    }
    return Mono.empty();
  }

  @Override
  public Mono<Void> validateHasCredit(KafkaConsumerListener kafkaConsumerListener, String customerId, KafkaTemplate<String, String> kafkaTemplate)  {
    return AccountConfigurationStrategy.super.validateHasCredit(kafkaConsumerListener, customerId, kafkaTemplate);
  }
}
