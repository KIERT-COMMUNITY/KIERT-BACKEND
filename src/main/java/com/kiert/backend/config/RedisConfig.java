// src/main/java/com/kiert/backend/config/RedisConfig.java
package com.kiert.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    @Bean
    @Primary
    public ObjectMapper restObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    private GenericJackson2JsonRedisSerializer buildSerializer() {
        return new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = buildSerializer();
        RedisCacheConfiguration defaultConfig = buildConfig(Duration.ofMinutes(5), serializer);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)

                // POSTS
                .withCacheConfiguration("posts", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("post", buildConfig(Duration.ofMinutes(10), serializer))

                // COMENTARIOS
                .withCacheConfiguration("comentarios", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("respuestas", buildConfig(Duration.ofMinutes(5), serializer))

                // REACCIONES
                .withCacheConfiguration("reaccionesPost", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("reaccionesComentario", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("reaccionesRespuesta", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuarioReaccionoPost", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuarioReaccionoComentario", buildConfig(Duration.ofMinutes(10), serializer))

                // PERFIL
                // ⚠️ "personalizacion", "marcos", "fondos" ELIMINADOS.
                //    Cachear PersonalizacionDTO como Object genérico rompía
                //    la deserialización ('@class' missing) y el PUT/GET entraba
                //    en bucle: guardaba → no podía leer → evict → guardaba.
                //    La personalización es UNA fila por usuario con índice UNIQUE,
                //    no compensa cachearla.
                .withCacheConfiguration("perfil", buildConfig(Duration.ofMinutes(30), serializer))

                // USUARIOS
                .withCacheConfiguration("usuarios", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("busquedaUsuarios", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("bloqueos", buildConfig(Duration.ofMinutes(10), serializer))

                // CHAT
                .withCacheConfiguration("conversaciones", buildConfig(Duration.ofSeconds(30), serializer))
                .withCacheConfiguration("mensajes", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("solicitudes", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("contactos", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuariosDisponibles", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("contadores", buildConfig(Duration.ofSeconds(30), serializer))

                // GRUPOS
                .withCacheConfiguration("gruposUsuario", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("gruposPublicos", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("grupo", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("miembrosGrupo", buildConfig(Duration.ofMinutes(3), serializer))
                .withCacheConfiguration("mensajesGrupo", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("historialGrupo", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("invitacionesPendientes", buildConfig(Duration.ofMinutes(1), serializer))
                .withCacheConfiguration("linksGrupo", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("infoInvitacion", buildConfig(Duration.ofMinutes(5), serializer))

                // NOTIFICACIONES
                .withCacheConfiguration("notificaciones", buildConfig(Duration.ofSeconds(60), serializer))
                .withCacheConfiguration("contadorNotificaciones", buildConfig(Duration.ofSeconds(30), serializer))

                // BIBLIOTECA
                .withCacheConfiguration("documentos", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("documento", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("categoriasDocumentos", buildConfig(Duration.ofHours(1), serializer))
                .withCacheConfiguration("documentosUsuario", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("recursosBiblioteca", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("recursoBiblioteca", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("recursosDestacados", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("categoriasBiblioteca", buildConfig(Duration.ofHours(1), serializer))
                .withCacheConfiguration("nivelesBiblioteca", buildConfig(Duration.ofHours(1), serializer))

                // COMPARTIDOS
                .withCacheConfiguration("compartidos", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("contadorCompartidos", buildConfig(Duration.ofMinutes(10), serializer))

                // REPORTES
                .withCacheConfiguration("resumenReportes", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("misReportes", buildConfig(Duration.ofMinutes(5), serializer))

                // BIENVENIDA
                .withCacheConfiguration("bienvenida", buildConfig(Duration.ofMinutes(30), serializer))

                .build();
    }

    private RedisCacheConfiguration buildConfig(Duration ttl, GenericJackson2JsonRedisSerializer serializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        GenericJackson2JsonRedisSerializer serializer = buildSerializer();
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

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache c, Object k) { log.warn("[CACHE] GET ({}::{})", c.getName(), k); }
            @Override public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) { log.warn("[CACHE] PUT ({}::{})", c.getName(), k); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object k) { log.warn("[CACHE] EVICT ({}::{})", c.getName(), k); }
            @Override public void handleCacheClearError(RuntimeException e, Cache c) { log.warn("[CACHE] CLEAR ({})", c.getName()); }
        };
    }
}