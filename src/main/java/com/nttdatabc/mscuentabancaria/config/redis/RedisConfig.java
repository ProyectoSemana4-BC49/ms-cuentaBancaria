package com.nttdatabc.mscuentabancaria.config.redis;

import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.model.DebitCard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
  @Bean("reactiveRedisTemplateAccount")
  public ReactiveRedisTemplate<String, Account> reactiveRedisTemplateAccount(ReactiveRedisConnectionFactory connectionFactory) {
    RedisSerializationContext<String, Account> serializationContext = RedisSerializationContext
        .<String, Account>newSerializationContext(new StringRedisSerializer())
        .key(new StringRedisSerializer())
        .value(new Jackson2JsonRedisSerializer<>(Account.class))
        .hashKey(new Jackson2JsonRedisSerializer<>(Integer.class))
        .hashValue(new GenericJackson2JsonRedisSerializer())
        .build();
    return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
  }
  @Bean("reactiveRedisTemplateDebitCard")
  public ReactiveRedisTemplate<String, DebitCard> reactiveRedisTemplateCardDebt(ReactiveRedisConnectionFactory connectionFactory) {
    RedisSerializationContext<String, DebitCard> serializationContext = RedisSerializationContext
        .<String, DebitCard>newSerializationContext(new StringRedisSerializer())
        .key(new StringRedisSerializer())
        .value(new Jackson2JsonRedisSerializer<>(DebitCard.class))
        .hashKey(new Jackson2JsonRedisSerializer<>(Integer.class))
        .hashValue(new GenericJackson2JsonRedisSerializer())
        .build();
    return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
  }
}
