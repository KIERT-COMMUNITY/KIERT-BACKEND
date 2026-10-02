// src/main/java/com/kiert/backend/config/RedisConfig.java
package com.kiert.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    // ============================================================
    // ⚠️ Este ObjectMapper es SOLO para Redis.
    //    NO usar @Primary (si no, la API REST también lo usaría y
    //    añadiría "@class" a las respuestas JSON).
    // ============================================================
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Solo el mapper de Redis lleva esto
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    private GenericJackson2JsonRedisSerializer buildSerializer(ObjectMapper mapper) {
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {

        GenericJackson2JsonRedisSerializer serializer = buildSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("posts",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("post",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("comments",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("perfil",
                        buildConfig(Duration.ofMinutes(30), serializer))
                .withCacheConfiguration("personalizacion",
                        buildConfig(Duration.ofMinutes(30), serializer))
                .withCacheConfiguration("marcos",
                        buildConfig(Duration.ofHours(24), serializer))
                .withCacheConfiguration("fondos",
                        buildConfig(Duration.ofHours(24), serializer))
                .withCacheConfiguration("usuarios",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("busquedaUsuarios",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("bloqueos",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("conversaciones",
                        buildConfig(Duration.ofSeconds(30), serializer))
                .withCacheConfiguration("mensajes",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("solicitudes",
                        buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("contactos",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuariosDisponibles",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("contadores",
                        buildConfig(Duration.ofSeconds(30), serializer))
                .withCacheConfiguration("notificaciones",
                        buildConfig(Duration.ofSeconds(60), serializer))
                .withCacheConfiguration("contadorNotificaciones",
                        buildConfig(Duration.ofSeconds(30), serializer))
                .withCacheConfiguration("recursosBiblioteca",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("categoriasBiblioteca",
                        buildConfig(Duration.ofHours(1), serializer))
                .build();
    }

    private RedisCacheConfiguration buildConfig(
            Duration ttl,
            GenericJackson2JsonRedisSerializer serializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer = buildSerializer(redisObjectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}