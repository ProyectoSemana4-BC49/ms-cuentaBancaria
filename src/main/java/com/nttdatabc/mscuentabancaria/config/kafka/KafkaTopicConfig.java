package com.nttdatabc.mscuentabancaria.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic generateTopicCustomer(){
    Map<String, String> configuration = new HashMap<>();
    configuration.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    configuration.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");
    configuration.put(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824");
    configuration.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012");
    return TopicBuilder.name("verify-customer-exist")
        .configs(configuration)
        .build();
  }
  @Bean
  public NewTopic generateTopicCredits(){
    Map<String, String> configuration = new HashMap<>();
    configuration.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    configuration.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");
    configuration.put(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824");
    configuration.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012");
    return TopicBuilder.name("get-credits-bycustomer")
        .configs(configuration)
        .build();
  }

  @Bean
  public NewTopic generateTopicHasDebt(){
    Map<String, String> configuration = new HashMap<>();
    configuration.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    configuration.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");
    configuration.put(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824");
    configuration.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012");
    return TopicBuilder.name("has-debt-credit")
        .configs(configuration)
        .build();
  }
  @Bean
  public NewTopic generateTopicHasCardDebt(){
    Map<String, String> configuration = new HashMap<>();
    configuration.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    configuration.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");
    configuration.put(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824");
    configuration.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012");
    return TopicBuilder.name("has-card-debt")
        .configs(configuration)
        .build();
  }
  @Bean
  public NewTopic generateTopicVerifyCardDebExist(){
    Map<String, String> configuration = new HashMap<>();
    configuration.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    configuration.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");
    configuration.put(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824");
    configuration.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012");
    return TopicBuilder.name("response-verify-carddeb-exist")
        .configs(configuration)
        .build();
  }
  @Bean
  public NewTopic generateTopicVerifyBalanceCardDebt(){
    Map<String, String> configuration = new HashMap<>();
    configuration.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    configuration.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");
    configuration.put(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824");
    configuration.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012");
    return TopicBuilder.name("response-verify-balance-carddeb")
        .configs(configuration)
        .build();
  }

}
