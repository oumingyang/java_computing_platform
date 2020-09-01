package org.inspur.redis;

import org.inspur.redis.RedisConnection;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConnectionUtil {
    public static RedisConnection create() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(8);
        jedisPoolConfig.setMaxIdle(8);
        jedisPoolConfig.setMinIdle(1);
        RedisConnection redisConnection = new RedisConnection();
        redisConnection.setIp("127.0.0.1");
        redisConnection.setPort(6379);
        redisConnection.setPwd("123456");
        redisConnection.setClientName(Thread.currentThread().getName());
        redisConnection.setTimeOut(10000);
        redisConnection.setJedisPoolConfig(jedisPoolConfig);
        return redisConnection;
    }
}