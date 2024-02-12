package com.nttdatabc.mscuentabancaria.config.kafka;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nttdatabc.mscuentabancaria.model.response.CreditExtDto;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import reactor.core.publisher.*;

/**
 * config consumer kafka listener.
 */
@Configuration
public class KafkaConsumerListener {
  private MonoSink<String> customerVerificationResponseSink;
  private MonoSink<String> customerHasCardDeb;
  private MonoSink<String> customerHasDebt;
  @Getter
  private List<CreditExtDto> creditList = new ArrayList<>();


  @KafkaListener(topics = {"response-verify-customer-exist"}, groupId = "my-group-id")
  public void listener(String message) {
    if (customerVerificationResponseSink != null) {
      customerVerificationResponseSink.success(message);
      customerVerificationResponseSink = null;
    }
  }

  public Mono<String> getCustomerVerificationResponse() {
    return Mono.create(sink -> customerVerificationResponseSink = sink);
  }

  @KafkaListener(topics = {"response-has-debt-credit"}, groupId = "my-group-id")
  public void listenerHasDebt(String message) {
    if (customerHasDebt != null) {
      customerHasDebt.success(message);
      customerHasDebt = null;
    }
  }

  public Mono<String> getCustomerHasDebtResponse() {
    return Mono.create(sink -> customerHasDebt = sink);
  }


  @KafkaListener(topics = {"response-get-credits-bycustomer"}, groupId = "my-group-id")
  public void listenerGetCreditsByCustomer(ConsumerRecord<String, String> record) {
    Gson gson = new Gson();
    String jsonData = record.value();
    creditList = gson.fromJson(jsonData, new TypeToken<List<CreditExtDto>>() {
    }.getType());
  }

  @KafkaListener(topics = {"response-has-card-debt"}, groupId = "my-group-id")
  public void listenerGetResponseHasCardDeb(String message) {
    if (customerHasCardDeb != null) {
      customerHasCardDeb.success(message);
      customerHasCardDeb = null;
    }
  }

  public Mono<String> getCustomerHasCardDeb() {
    return Mono.create(sink -> customerHasCardDeb = sink);
  }

}
