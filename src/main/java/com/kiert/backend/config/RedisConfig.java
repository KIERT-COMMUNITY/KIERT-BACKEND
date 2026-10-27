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

    // ============================================================
    // OBJECT MAPPER PARA REDIS
    // ============================================================
    /**
     * ObjectMapper EXCLUSIVO para Redis:
     *
     *  - JavaTimeModule → soporta Instant, LocalDateTime, etc.
     *  - defaultTyping NON_FINAL con @class → permite reconstruir tipos
     *    (ArrayList, HashMap, DTOs, etc.) al deserializar.
     *  - Visibility ANY → serializa campos privados.
     *
     * ⚠️ IMPORTANTE:
     * Se activa defaultTyping aquí porque VAMOS A PASAR este mapper
     * al GenericJackson2JsonRedisSerializer. Si lo dejamos al
     * constructor sin argumentos, se crea un mapper interno SIN
     * JavaTimeModule y vuelve a fallar con Instant.
     */
    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Soporte para java.time
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Serializar campos privados
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // Tipo embebido (@class) — necesario para Redis
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * ObjectMapper @Primary para REST (SIN defaultTyping → sin "@class"
     * en las respuestas HTTP).
     */
    @Bean
    @Primary
    public ObjectMapper restObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    // ============================================================
    // SERIALIZADOR
    // ============================================================
    /**
     * Serializer Redis que SÍ conoce JavaTimeModule, porque recibe
     * nuestro ObjectMapper ya preparado con defaultTyping + JavaTime.
     */
    private GenericJackson2JsonRedisSerializer buildSerializer() {
        return new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());
    }

    // ============================================================
    // CACHE MANAGER
    // ============================================================
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        GenericJackson2JsonRedisSerializer serializer = buildSerializer();

        RedisCacheConfiguration defaultConfig = buildConfig(Duration.ofMinutes(5), serializer);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)

                .withCacheConfiguration("posts", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("post", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("comments", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("respuestas", buildConfig(Duration.ofMinutes(5), serializer))

                .withCacheConfiguration("reaccionesPost", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("reaccionesComentario", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuarioReaccionoPost", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuarioReaccionoComentario", buildConfig(Duration.ofMinutes(10), serializer))

                .withCacheConfiguration("perfil", buildConfig(Duration.ofMinutes(30), serializer))
                .withCacheConfiguration("personalizacion", buildConfig(Duration.ofMinutes(30), serializer))
                .withCacheConfiguration("marcos", buildConfig(Duration.ofHours(24), serializer))
                .withCacheConfiguration("fondos", buildConfig(Duration.ofHours(24), serializer))

                .withCacheConfiguration("usuarios", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("busquedaUsuarios", buildConfig(Duration.ofMinutes(5), serializer))

                .withCacheConfiguration("bloqueos", buildConfig(Duration.ofMinutes(10), serializer))

                .withCacheConfiguration("conversaciones", buildConfig(Duration.ofSeconds(30), serializer))
                .withCacheConfiguration("mensajes", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("solicitudes", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("contactos", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("usuariosDisponibles", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("contadores", buildConfig(Duration.ofSeconds(30), serializer))

                .withCacheConfiguration("gruposUsuario", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("gruposPublicos", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("grupo", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("miembrosGrupo", buildConfig(Duration.ofMinutes(3), serializer))
                .withCacheConfiguration("mensajesGrupo", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("historialGrupo", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("invitacionesPendientes", buildConfig(Duration.ofMinutes(1), serializer))
                .withCacheConfiguration("linksGrupo", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("infoInvitacion", buildConfig(Duration.ofMinutes(5), serializer))

                .withCacheConfiguration("notificaciones", buildConfig(Duration.ofSeconds(60), serializer))
                .withCacheConfiguration("contadorNotificaciones", buildConfig(Duration.ofSeconds(30), serializer))

                .withCacheConfiguration("documentos", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("documento", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("categoriasDocumentos", buildConfig(Duration.ofHours(1), serializer))
                .withCacheConfiguration("documentosUsuario", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("recursosBiblioteca", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("recursoBiblioteca", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("recursosDestacados", buildConfig(Duration.ofMinutes(10), serializer))
                .withCacheConfiguration("categoriasBiblioteca", buildConfig(Duration.ofHours(1), serializer))
                .withCacheConfiguration("nivelesBiblioteca", buildConfig(Duration.ofHours(1), serializer))

                .withCacheConfiguration("compartidos", buildConfig(Duration.ofMinutes(5), serializer))
                .withCacheConfiguration("contadorCompartidos", buildConfig(Duration.ofMinutes(10), serializer))

                .withCacheConfiguration("resumenReportes", buildConfig(Duration.ofMinutes(2), serializer))
                .withCacheConfiguration("misReportes", buildConfig(Duration.ofMinutes(5), serializer))

                // ✅ Cache de BIENVENIDA (no necesita invalidación frecuente)
                .withCacheConfiguration("bienvenida", buildConfig(Duration.ofMinutes(30), serializer))

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

    // ============================================================
    // REDIS TEMPLATE
    // ============================================================
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

    // ============================================================
    // STRING REDIS TEMPLATE
    // ============================================================
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    // ============================================================
    // ERROR HANDLER — Resiliencia + limpieza de claves corruptas
    // ============================================================
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[CACHE] Error en GET ({}::{}): {}", cache.getName(), key, exception.getMessage());
                try {
                    cache.evict(key);
                } catch (Exception ignored) {}
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("[CACHE] Error en PUT ({}::{}): {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[CACHE] Error en EVICT ({}::{}): {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("[CACHE] Error en CLEAR ({}): {}", cache.getName(), exception.getMessage());
            }
        };
    }
}