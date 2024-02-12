package com.nttdatabc.mscuentabancaria.service;

import static com.nttdatabc.mscuentabancaria.utils.AccountValidator.*;
import static com.nttdatabc.mscuentabancaria.utils.Constantes.EX_ERROR_REQUEST;
import static com.nttdatabc.mscuentabancaria.utils.Constantes.EX_NOT_FOUND_RECURSO;

import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.model.enums.TypeAccountBank;
import com.nttdatabc.mscuentabancaria.model.enums.TypeCustomer;
import com.nttdatabc.mscuentabancaria.repository.AccountRepository;
import com.nttdatabc.mscuentabancaria.service.interfaces.AccountService;
import com.nttdatabc.mscuentabancaria.service.strategy.strategy_account.AccountValidationStrategy;
import com.nttdatabc.mscuentabancaria.service.strategy.strategy_account.EmpresaAccountValidationStrategy;
import com.nttdatabc.mscuentabancaria.service.strategy.strategy_account.PersonaAccountValidationStrategy;
import com.nttdatabc.mscuentabancaria.service.strategy.strategy_typeaccount.*;
import com.nttdatabc.mscuentabancaria.utils.AccountValidator;
import com.nttdatabc.mscuentabancaria.utils.Utilitarios;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Account service implement.
 */
@Service
@Slf4j
public class AccountServiceImpl implements AccountService {
  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private AccountValidator accountValidator;
  @Autowired
  private KafkaConsumerListener kafkaConsumerListener;
  @Autowired
  @Qualifier("reactiveRedisTemplateAccount")
  private ReactiveRedisTemplate<String, Account> redisTemplate;

  @Override
  public Flux<Account> getAllAccountsService() {
    String cacheKey = "accounts";
    Duration cacheDuration = Duration.ofSeconds(50);
    return redisTemplate.opsForList().range(cacheKey, 0, -1)
        .switchIfEmpty(
            accountRepository.findAll()
                .flatMap(account -> redisTemplate.opsForList().leftPushAll(cacheKey, account)
                    .thenMany(Flux.just(account))))
        .cache(cacheDuration)
        .doOnSubscribe(subscription -> redisTemplate.expire(cacheKey, cacheDuration).subscribe());
  }

  @Override
  public Mono<Void> createAccountService(Account account) {

    return validateAccountsNoNulls(account)
        .then(validateAccountEmpty(account))
        .then(verifyTypeAccount(account))
        .then(verifyValues(account))
        .then(accountValidator.verifyCustomerDebCredit(account.getCustomerId(), kafkaConsumerListener))
        .then(Mono.just(account))
        .flatMap(accountMono -> accountValidator.verifyCustomerExists(accountMono.getCustomerId(), kafkaConsumerListener)
            .flatMap(customerFound -> accountRepository.findByCustomerId(customerFound.get_id()).collectList()
                .flatMap(listAccountByCustomer -> {
                  AccountValidationStrategy accountValidationStrategy = null;
                  if (customerFound.getType().equalsIgnoreCase(TypeCustomer.PERSONA.toString())) {
                    accountValidationStrategy = new PersonaAccountValidationStrategy();
                    return accountValidationStrategy.validateAccount(account, listAccountByCustomer)
                        .thenReturn(customerFound);
                  } else {
                    accountValidationStrategy = new EmpresaAccountValidationStrategy();
                    return accountValidationStrategy.validateAccount(account, listAccountByCustomer)
                        .thenReturn(customerFound);
                  }
                })
            )).flatMap(customerFound -> {
              AccountConfigurationStrategy configationStrategy = null;
              if (account.getTypeAccount().equalsIgnoreCase(TypeAccountBank.AHORRO.toString())) {
                configationStrategy = new AhorroAccountConfigurationStrategy();
                return configationStrategy.configureAccount(account, customerFound);
              } else if (account.getTypeAccount().equalsIgnoreCase(TypeAccountBank.CORRIENTE.toString())) {
                configationStrategy = new CorrienteAccountConfigurationStrategy();
                return configationStrategy.configureAccount(account, customerFound);
              } else if (account.getTypeAccount().equalsIgnoreCase(TypeAccountBank.PLAZO_FIJO.toString())) {
                configationStrategy = new PlazoFijoAccountConfigurationStrategy();
                return configationStrategy.configureAccount(account, customerFound);
              } else if (account.getTypeAccount().equalsIgnoreCase(TypeAccountBank.VIP.toString())) {
                configationStrategy = new VipAccountConfigurationStrategy();
                return configationStrategy.validateHasCredit(kafkaConsumerListener, customerFound.get_id(), kafkaTemplate)
                    .then(configationStrategy.configureAccount(account, customerFound));
              } else if (account.getTypeAccount().equalsIgnoreCase(TypeAccountBank.PYME.toString())) {
                configationStrategy = new PymeAccountConfigurationStrategy();
                return configationStrategy.configureAccount(account, customerFound)
                    .then(configationStrategy.validateHasCorriente(accountRepository.findByCustomerId(customerFound.get_id())));
              } else {
                return Mono.error(() -> new ErrorResponseException(EX_ERROR_REQUEST, HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST));
              }
            })
        .then(Mono.just(account))
        .doOnNext(accountFlujo -> accountFlujo.setId(Utilitarios.generateUuid()))
        .flatMap(accountRepository::save)
        .then();

  }

  @Override
  public Mono<Void> updateAccountServide(Account account) {
    return validateAccountsNoNulls(account)
        .then(validateAccountEmpty(account))
        .then(verifyTypeAccount(account))
        .then(Mono.just(account))
        .flatMap(accountRequest -> getAccountByIdService(accountRequest.getId()))
        .map(accountFound -> {
          accountFound.setTypeAccount(account.getTypeAccount());
          accountFound.setCurrentBalance(account.getCurrentBalance());
          accountFound.setCustomerId(account.getCustomerId());
          accountFound.setHolders(account.getHolders());
          accountFound.setDateMovement(account.getDateMovement());
          accountFound.setLimitMaxMovements(account.getLimitMaxMovements());
          accountFound.setMaintenanceFee(account.getMaintenanceFee());
          return accountFound;
        })
        .flatMap(accountRepository::save)
        .then();
  }

  @Override
  public Mono<Void> deleteAccountByIdService(String accountId) {
    return getAccountByIdService(accountId)
        .flatMap(account -> accountRepository.delete(account))
        .then();
  }

  @Override
  public Mono<Account> getAccountByIdService(String accountId) {
    return accountRepository.findById(accountId)
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_NOT_FOUND_RECURSO,
            HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)));
  }

  @Override
  public Flux<Account> getAccountsByCustomerIdService(String customerId) {
    return accountValidator.verifyCustomerExists(customerId, kafkaConsumerListener)
        .thenMany(accountRepository.findByCustomerId(customerId));
  }


}
