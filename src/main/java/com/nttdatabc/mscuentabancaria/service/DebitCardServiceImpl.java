package com.nttdatabc.mscuentabancaria.service;

import static com.nttdatabc.mscuentabancaria.utils.Constantes.EX_NOT_FOUND_RECURSO;
import static com.nttdatabc.mscuentabancaria.utils.DebitCardValidator.*;
import static com.nttdatabc.mscuentabancaria.utils.Utilitarios.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netflix.discovery.converters.Auto;
import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.helpers.AccountsSecundary;
import com.nttdatabc.mscuentabancaria.model.DebitCard;
import com.nttdatabc.mscuentabancaria.repository.AccountRepository;
import com.nttdatabc.mscuentabancaria.repository.DebitCardRepository;
import com.nttdatabc.mscuentabancaria.service.interfaces.DebitCardService;
import com.nttdatabc.mscuentabancaria.utils.DebitCardValidator;
import com.nttdatabc.mscuentabancaria.utils.exceptions.errors.ErrorResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Debit card service.
 */
@Service
@Slf4j
public class DebitCardServiceImpl implements DebitCardService {
  @Autowired
  private DebitCardRepository debitCardRepository;
  @Autowired
  private AccountServiceImpl accountService;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired
  private DebitCardValidator debitCardValidator;
  @Autowired
  private KafkaConsumerListener kafkaConsumerListener;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  @Qualifier("reactiveRedisTemplateDebitCard")
  private ReactiveRedisTemplate<String, DebitCard> redisTemplate;

  @Override
  public Flux<DebitCard> getAllDebitCardService() {
    String cacheKey = "carddebt";
    Duration cacheDuration = Duration.ofSeconds(50);
    return redisTemplate.opsForList().range(cacheKey,0, -1)
        .switchIfEmpty(
            debitCardRepository.findAll()
                .flatMap(debitCard -> redisTemplate.opsForList().leftPushAll(cacheKey, debitCard)
                    .thenMany(Flux.just(debitCard))))
        .cache(cacheDuration)
        .doOnSubscribe(subscription -> redisTemplate.expire(cacheKey, cacheDuration).subscribe());
  }

  @Override
  public Mono<Void> createDebitCardService(DebitCard debitCard) {
    return validateDebitCardNoNulls(debitCard)
        .then(validateDebitCardEmpty(debitCard))
        .then(verifyDuplicateNumberDebitCard(debitCard.getNumberCard(), debitCardRepository))
        .then(debitCardValidator.verifyCustomerExists(debitCard.getCustomerId(), kafkaConsumerListener))
        .then(debitCardValidator.verifyCustomerDebCredit(debitCard.getCustomerId(), kafkaConsumerListener))
        .then(accountRepository.findById(debitCard.getAccountIdPrincipal()))
        .then(Mono.just(debitCard))
        .flatMap(debitCardFlujo -> {
          if (debitCardFlujo.getAccountsSecundary() != null) {
            return Flux.fromIterable(debitCardFlujo.getAccountsSecundary())
                .flatMap(accountsSecundary -> accountRepository.findById(accountsSecundary.getAccountId())
                    .hasElement()
                    .defaultIfEmpty(false))
                .any(Boolean::booleanValue)
                .defaultIfEmpty(false);
          } else {
            return Mono.empty();
          }

        }).then(Mono.just(debitCard))
        .map(debitCardTransform -> {
          LocalDateTime dateTimeNow = LocalDateTime.now();
          debitCardTransform.setNumberCard(generateNumberCard());
          debitCardTransform.setCvv2(generateRandomCVV2());
          debitCardTransform.setCreatedCardDebit(dateTimeNow.toString());
          debitCardTransform.setExpiration(calculateExpirationDate());
          debitCardTransform.set_id(generateUuid());
          if (debitCardTransform.getAccountsSecundary() == null) {
            debitCardTransform.setAccountsSecundary(new ArrayList<>());
          }
          return debitCardTransform;
        }).flatMap(debitCardSave -> debitCardRepository.save(debitCardSave))
        .then();

  }

  @Override
  public Mono<Void> deleteDebitCardService(String debitCardId) {
    return getDebitCardByIdService(debitCardId)
        .flatMap(debitCard -> debitCardRepository.delete(debitCard));
  }

  @Override
  public Mono<DebitCard> getDebitCardByIdService(String debitCardId) {
    return debitCardRepository.findById(debitCardId)
        .switchIfEmpty(Mono.error(new ErrorResponseException(EX_NOT_FOUND_RECURSO, HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)));
  }

  @Override
  public Mono<Void> updateDebitCardService(DebitCard debitCard) {
    return validateDebitCardUpdateNoNulls(debitCard)
        .then(validateDebitCardUpdateEmpty(debitCard))
        .then(getDebitCardByIdService(debitCard.get_id())
            .flatMap(existingDebitCard -> {
              Flux<AccountsSecundary> accountsFlux = Flux.fromIterable(debitCard.getAccountsSecundary());
              Mono<DebitCard> accountFlux = accountsFlux.flatMap(accountsSecundary -> accountService.getAccountByIdService(accountsSecundary.getAccountId()))
                  .collectList()
                  .flatMap(accounts -> {
                    boolean allCustomersExist = accounts.stream()
                        .allMatch(Objects::nonNull);
                    if (!allCustomersExist) {
                      return Mono.error(new Exception("Algunos customerId no existen"));
                    }

                    List<AccountsSecundary> combinedList = new ArrayList<>(existingDebitCard.getAccountsSecundary());
                    combinedList.addAll(debitCard.getAccountsSecundary());
                    existingDebitCard.setAccountsSecundary(combinedList);
                    return debitCardRepository.save(existingDebitCard);
                  });

              return accountFlux;
            })
        )
        .then();
  }
  @KafkaListener(topics = {"verify-carddeb-exist"}, groupId = "my-group-id")
  public void listenerVerifyCustomerCredit(String message){
    Gson gson = new Gson();
    Map<String, String> map = gson.fromJson(message, new TypeToken<Map<String, String>>(){}.getType());
    String cardDebitAssociate = map.get("cardDebitAssociate");

    Mono<DebitCard> getDebitCard = getDebitCardByIdService(cardDebitAssociate);

    getDebitCard.subscribe(
        customer -> {
          String jsonDataMono = gson.toJson(customer);
          kafkaTemplate.send("response-verify-carddeb-exist", jsonDataMono);
        },
        throwable -> {
          Map<String, String> responseError = new HashMap<>();
          responseError.put("error", throwable.getMessage());
          String responseString = gson.toJson(responseError);
          kafkaTemplate.send("response-verify-carddeb-exist", responseString);
        }
    );

  }

  @KafkaListener(topics = {"update-amount-carddeb"}, groupId = "my-group-id")
  public void listenerUpdateAmountCardDeb(String message){
    Gson gson = new Gson();
    Map<String, String> map = gson.fromJson(message, new TypeToken<Map<String, String>>(){}.getType());
    String cardDebitAssociate = map.get("cardDebitId");
    String type = map.get("type");
    Double amountUpdate = Double.parseDouble(map.get("mount"));
    debitCardRepository.findById(cardDebitAssociate)
        .flatMap(debitCard -> {
          String accountPrincipalId = debitCard.getAccountIdPrincipal();
          return accountRepository.findById(accountPrincipalId)
              .flatMap(account -> {
                if(type.equals("increase")){
                  account.setCurrentBalance(account.getCurrentBalance().add(BigDecimal.valueOf(amountUpdate)));
                }else{
                  account.setCurrentBalance(account.getCurrentBalance().subtract(BigDecimal.valueOf(amountUpdate)));
                }
                return accountRepository.save(account);
              });
        }).subscribe();
  }
  @KafkaListener(topics = {"verify-balance-carddeb"}, groupId = "my-group-id")
  public void listenerVerifyBalanceCardDeb(String message){
    Gson gson = new Gson();
    Map<String, String> map = gson.fromJson(message, new TypeToken<Map<String, String>>(){}.getType());
    String cardDebitAssociate = map.get("cardDebitId");
    Double amountUpdate = Double.parseDouble(map.get("mount"));

    debitCardRepository.findById(cardDebitAssociate)
        .flatMap(debitCard -> {
          String accountPrincipalId = debitCard.getAccountIdPrincipal();
          return accountRepository.findById(accountPrincipalId)
              .flatMap(account -> {
                Map<String, String> requestResponseVerifyBalance = new HashMap<>();
                if(account.getCurrentBalance().doubleValue() >= amountUpdate){
                  requestResponseVerifyBalance.put("response", "ok");
                }else{
                  requestResponseVerifyBalance.put("response", "error");
                }
                kafkaTemplate.send("response-verify-balance-carddeb", gson.toJson(requestResponseVerifyBalance));
                return Mono.empty();
              });
        }).subscribe();
  }

}
