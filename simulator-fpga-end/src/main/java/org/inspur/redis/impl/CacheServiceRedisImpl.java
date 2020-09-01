package org.inspur.redis.impl;

import org.inspur.redis.CacheService;
import org.inspur.redis.RedisConnection;
import org.inspur.redis.SerializeUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

public class CacheServiceRedisImpl implements CacheService {
    private static Log log = LogFactory.getLog(CacheServiceRedisImpl.class);

    private RedisConnection redisConnection;

    // 数据库索引值
    private Integer dbIndex;

    public void setRedisConnection(RedisConnection redisConnection) {
        this.redisConnection = redisConnection;
    }

    public void setDbIndex(Integer dbIndex) {
        this.dbIndex = dbIndex;
    }

    public void putObject(String key, Object value) {
        putObject(key, value, -1);
    }

    public void putByteArray(String key, byte[] value)
    {
        putByteArray(key, value, -1);
    }

    public void putObject(String key, Object value, int expiration) {

        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            if (expiration > 0) {
                jedis.setex(key.getBytes(), expiration, SerializeUtil.serialize(value));
            } else {
                jedis.set(key.getBytes(), SerializeUtil.serialize(value));
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public void putByteArray(String key, byte[] value, int expiration) {

        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            if (expiration > 0) {
                jedis.setex(key.getBytes(), expiration, value);
            } else {
                jedis.set(key.getBytes(), value);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public Object pullObject(String key) {

        log.trace("strat find cache with " + key);
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            byte[] result = jedis.get(key.getBytes());
            if (result == null) {
                log.trace("can not find caceh with " + key);
                return null;
            } else {
                log.trace("find cache success with " + key);
                return SerializeUtil.unserialize(result);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return null;
    }

    public byte[] pullByteArray(String key) {

        log.trace("strat find cache with " + key);
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            byte[] result = jedis.get(key.getBytes());
            if (result == null) {
                log.trace("can not find caceh with " + key);
                return null;
            } else {
                log.trace("find cache success with " + key);
                return result;
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }

    public boolean expire(String key, int expireSecond) {
        log.trace("strat set expire " + key);
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            return jedis.expire(key, expireSecond) == 1;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    public Long ttl(String key) {
        log.trace("get set expire " + key);
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            return jedis.ttl(key);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return -2L;
    }

    public boolean delObject(String key) {
        log.trace("strat delete cache with " + key);
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            return jedis.del(key.getBytes()) > 0;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return false;
    }

    public void clearObject() {

        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            jedis.flushDB();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}