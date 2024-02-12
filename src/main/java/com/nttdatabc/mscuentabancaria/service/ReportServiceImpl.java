package com.nttdatabc.mscuentabancaria.service;

import static com.nttdatabc.mscuentabancaria.utils.MovementValidator.validateAccountRegister;

import com.google.gson.Gson;
import com.nttdatabc.mscuentabancaria.config.kafka.KafkaConsumerListener;
import com.nttdatabc.mscuentabancaria.model.*;
import com.nttdatabc.mscuentabancaria.model.helpers.BalanceAccounts;
import com.nttdatabc.mscuentabancaria.model.helpers.SummaryAccountBalance;
import com.nttdatabc.mscuentabancaria.model.helpers.SummaryProductsBank;
import com.nttdatabc.mscuentabancaria.model.response.CreditExtDto;
import com.nttdatabc.mscuentabancaria.repository.AccountRepository;
import com.nttdatabc.mscuentabancaria.repository.MovementDebitCardRepository;
import com.nttdatabc.mscuentabancaria.repository.MovementRepository;
import com.nttdatabc.mscuentabancaria.service.interfaces.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.nttdatabc.mscuentabancaria.utils.AccountValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * ReportService impl.
 */
@Service
public class ReportServiceImpl implements ReportService {
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private MovementRepository movementRepository;

  @Autowired
  private AccountServiceImpl accountServiceImpl;
  @Autowired
  private MovementDebitCardRepository movementDebitCardRepository;
  @Autowired
  private DebitCardServiceImpl debitCardService;

  @Autowired
  private AccountValidator accountValidator;
  @Autowired
  private KafkaConsumerListener kafkaConsumerListener;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public Mono<BalanceAccounts> getBalanceAverageService(String customerId) {
    return accountValidator.verifyCustomerExists(customerId, kafkaConsumerListener)
        .flatMap(customerExt -> {
          BalanceAccounts balanceAccounts = new BalanceAccounts();
          balanceAccounts.setCustomerId(customerId);
          LocalDate currentDate = LocalDate.now();
          int daysInMonth = currentDate.lengthOfMonth();
          int year = LocalDate.now().getYear();
          int month = LocalDate.now().getMonthValue();
          String dateFilter = String.format("%d-%s", year, String.valueOf(month).length() == 1 ? "0" + month : month);
          return accountRepository.findByCustomerId(customerId)
              .flatMap(account -> movementRepository.findByAccountId(account.getId())
                  .filter(movement -> movement.getFecha().contains(dateFilter))
                  .collectList()
                  .map(movementsInCurrentMonth -> {
                    double totalBalance = movementsInCurrentMonth.stream()
                        .mapToDouble(movement -> movement.getMount().doubleValue())
                        .sum();

                    BigDecimal averageDailyBalance = BigDecimal.valueOf(totalBalance / daysInMonth);

                    SummaryAccountBalance summaryAccountBalance = new SummaryAccountBalance();
                    summaryAccountBalance.setAccountId(account.getId());
                    summaryAccountBalance.setBalanceAvg(averageDailyBalance);

                    return summaryAccountBalance;
                  })).collectList()
              .doOnSuccess(summaryAccountBalances -> balanceAccounts.setSummaryAccounts(summaryAccountBalances))
              .thenReturn(balanceAccounts);
        });

  }

  @Override
  public Flux<Movement> getMovementsWithFee(String accountId) {
    int year = LocalDate.now().getYear();
    int mounth = LocalDate.now().getMonthValue();
    String dateFilter = String.format("%d-%s", year, String.valueOf(mounth).length() == 1 ? "0" + mounth : mounth);

    return validateAccountRegister(accountId, accountServiceImpl)
        .thenMany(movementRepository.findByAccountId(accountId))
        .filter(movement -> movement.getFee().doubleValue() > 0)
        .filter(movement -> movement.getFecha().contains(dateFilter));


  }

  @Override
  public Flux<MovementDebitCard> getMovementsDebitCardLastTen(String debitCardId) {
    return debitCardService.getDebitCardByIdService(debitCardId)
        .thenMany(movementDebitCardRepository.findTop10ByOrderByDateDesc(debitCardId));

  }

  @Override
  public Mono<Account> getAccountMainDebitCardService(String debitCardId) {
    return debitCardService.getDebitCardByIdService(debitCardId)
        .flatMap(debitCard -> accountServiceImpl.getAccountByIdService(debitCard.getAccountIdPrincipal()));
  }

  @Override
  public Mono<SummaryProductsBank> getSummaryProductsBankService(String customerId) {

    return accountValidator.verifyCustomerExists(customerId, kafkaConsumerListener)
        .flatMap(customerExt -> {
          Flux<Account> accountFlux = accountRepository.findByCustomerId(customerExt.get_id());
          Flux<CreditExtDto> creditExtFlux = getCreditsByCustomer(customerId);

          return Mono.zip(accountFlux.collectList(), creditExtFlux.collectList())
              .map(tuple -> {
                SummaryProductsBank summaryProductsBank = new SummaryProductsBank();
                summaryProductsBank.setCustormerId(customerExt.get_id());
                summaryProductsBank.setAccountsBanks(tuple.getT1());
                summaryProductsBank.setCreditsBanks(tuple.getT2());
                return summaryProductsBank;
              });
        });
  }

  public Flux<CreditExtDto> getCreditsByCustomer(String customerId){
    Map<String, String> request = new HashMap<>();
    request.put("customerId", customerId);
    Gson gson = new Gson();
    String customerIdJson = gson.toJson(request);
    kafkaTemplate.send("get-credits-bycustomer", customerIdJson);
    return Flux.fromIterable(kafkaConsumerListener.getCreditList());
  }


}
