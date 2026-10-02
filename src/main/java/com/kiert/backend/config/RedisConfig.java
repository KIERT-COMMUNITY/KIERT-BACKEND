// src/main/java/com/kiert/backend/config/RedisConfig.java
package com.kiert.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

    // ============================================================
    // OBJECT MAPPER PARA REDIS
    // ============================================================
    /**
     * Este ObjectMapper es SOLO para Redis.
     * NO lleva @Primary, así Spring Boot crea el suyo propio para REST
     * (sin "@class" en las respuestas JSON).
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Solo este mapper lleva activateDefaultTyping
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * ObjectMapper @Primary para REST.
     * Spring Boot lo usará para serializar las respuestas HTTP.
     * SIN activateDefaultTyping → las respuestas NO llevan "@class".
     */
    @Bean
    @Primary
    public ObjectMapper restObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // NO llamar activateDefaultTyping aquí
        return mapper;
    }

    // ============================================================
    // SERIALIZADOR REUTILIZABLE
    // ============================================================
    private GenericJackson2JsonRedisSerializer buildSerializer(ObjectMapper mapper) {
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    // ============================================================
    // CACHE MANAGER
    // ============================================================
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {

        GenericJackson2JsonRedisSerializer serializer = buildSerializer(redisObjectMapper);

        // Configuración por defecto (para cachés no definidas explícitamente)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)

                // ================================================
                // POSTS Y COMENTARIOS
                // ================================================
                .withCacheConfiguration("posts",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("post",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("comments",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("respuestas",
                        buildConfig(Duration.ofMinutes(5), serializer))

                // ================================================
                // REACCIONES
                // ================================================
                .withCacheConfiguration("reaccionesPost",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("reaccionesComentario",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuarioReaccionoPost",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuarioReaccionoComentario",
                        buildConfig(Duration.ofMinutes(10), serializer))

                // ================================================
                // PERFIL Y PERSONALIZACIÓN
                // ================================================
                .withCacheConfiguration("perfil",
                        buildConfig(Duration.ofMinutes(30), serializer))
                .withCacheConfiguration("personalizacion",
                        buildConfig(Duration.ofMinutes(30), serializer))
                .withCacheConfiguration("marcos",
                        buildConfig(Duration.ofHours(24), serializer))
                .withCacheConfiguration("fondos",
                        buildConfig(Duration.ofHours(24), serializer))

                // ================================================
                // USUARIOS
                // ================================================
                .withCacheConfiguration("usuarios",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("busquedaUsuarios",
                        buildConfig(Duration.ofMinutes(5), serializer))

                // ================================================
                // BLOQUEOS
                // ================================================
                .withCacheConfiguration("bloqueos",
                        buildConfig(Duration.ofMinutes(10), serializer))

                // ================================================
                // CHAT PRIVADO
                // ================================================
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

                // ================================================
                // CHAT GRUPAL
                // ================================================
                .withCacheConfiguration("gruposUsuario",
                        buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("gruposPublicos",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("grupo",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("miembrosGrupo",
                        buildConfig(Duration.ofMinutes(3), serializer))
                .withCacheConfiguration("mensajesGrupo",
                        buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("historialGrupo",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("invitacionesPendientes",
                        buildConfig(Duration.ofMinutes(1), serializer))
                .withCacheConfiguration("linksGrupo",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("infoInvitacion",
                        buildConfig(Duration.ofMinutes(5), serializer))

                // ================================================
                // NOTIFICACIONES
                // ================================================
                .withCacheConfiguration("notificaciones",
                        buildConfig(Duration.ofSeconds(60), serializer))
                .withCacheConfiguration("contadorNotificaciones",
                        buildConfig(Duration.ofSeconds(30), serializer))

                // ================================================
                // DOCUMENTOS Y BIBLIOTECA
                // ================================================
                .withCacheConfiguration("documentos",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("documento",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("categoriasDocumentos",
                        buildConfig(Duration.ofHours(1), serializer))
                .withCacheConfiguration("documentosUsuario",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("recursosBiblioteca",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("recursoBiblioteca",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("recursosDestacados",
                        buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("categoriasBiblioteca",
                        buildConfig(Duration.ofHours(1), serializer))
                .withCacheConfiguration("nivelesBiblioteca",
                        buildConfig(Duration.ofHours(1), serializer))

                // ================================================
                // COMPARTIDOS
                // ================================================
                .withCacheConfiguration("compartidos",
                        buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("contadorCompartidos",
                        buildConfig(Duration.ofMinutes(10), serializer))

                // ================================================
                // REPORTES
                // ================================================
                .withCacheConfiguration("resumenReportes",
                        buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("misReportes",
                        buildConfig(Duration.ofMinutes(5), serializer))

                .build();
    }

    // ============================================================
    // HELPER PARA CONFIGURACIONES INDIVIDUALES
    // ============================================================
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

    // ============================================================
    // REDIS TEMPLATE (para operaciones manuales con objetos)
    // ============================================================
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

    // ============================================================
    // STRING REDIS TEMPLATE (contadores, rate limiting, blacklist)
    // ============================================================
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    // ============================================================
    // ERROR HANDLER — Resiliencia ante caída de Redis
    // ============================================================
    /**
     * Si Redis cae, en lugar de tumbar la app, logueamos el error
     * y la operación se ejecuta sin caché.
     *
     * CRÍTICO para producción.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[CACHE] Error en GET ({}::{}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("[CACHE] Error en PUT ({}::{}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[CACHE] Error en EVICT ({}::{}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("[CACHE] Error en CLEAR ({}): {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}