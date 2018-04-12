package com.mmall.util;

import com.mmall.common.RedisPool;
import com.mmall.common.RedisShardedPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;

/**
 * Created by Administrator on 2018/4/11.
 */
@Slf4j
public class RedisShardedPoolUtil {


    public static String set(String key, String value){
        ShardedJedis jedis = null;
        String result = null;
        try {
            jedis = RedisShardedPool.getJedis();
            result = jedis.set(key, value);
        }catch (Exception e){
            log.error("set key:{} value:{} error", key, value, e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    public static String get(String key){
        ShardedJedis jedis = null;
        String result = null;
        try {
            jedis = RedisShardedPool.getJedis();
            result = jedis.get(key);
        }catch (Exception e){
            log.error("get key:{} error", key, e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    /**
     * set一个带有效时间的key value
     * @param key
     * @param value
     * @param exTime 秒
     * @return
     */
    public static String setEx(String key, String value, int exTime){
        ShardedJedis jedis = null;
        String result = null;
        try {
            jedis = RedisShardedPool.getJedis();
            result = jedis.setex(key, exTime, value);
        }catch (Exception e){
            log.error("setEx key:{} value:{} exTime:{} error", key, value, exTime, e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    /**
     * 设置key的有效时间
     * @param key
     * @param exTime 秒
     * @return
     */
    public static Long expire(String key, int exTime){
        ShardedJedis jedis = null;
        Long result = null;
        try {
            jedis = RedisShardedPool.getJedis();
            result = jedis.expire(key, exTime);
        }catch (Exception e){
            log.error("expire key:{} error", key, e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    public static Long del(String key){
        ShardedJedis jedis = null;
        Long result = null;
        try {
            jedis = RedisShardedPool.getJedis();
            result = jedis.del(key);
        }catch (Exception e){
            log.error("del key:{} error", key, e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }


    public static void main(String[] args) {
        ShardedJedis jedis = RedisShardedPool.getJedis();
//        RedisShardedPoolUtil.set("testKey", "testValue");
//        String value = RedisShardedPoolUtil.get("testKey");
//        RedisShardedPoolUtil.setEx("testKey1", "testValue1", 60*10);
//        RedisShardedPoolUtil.expire("testKey", 60*20);
//        RedisShardedPoolUtil.del("testKey");

        for(int i=0; i<10; i++){
            RedisShardedPoolUtil.set("keys"+i, "values"+i);
        }

        System.out.println("end");
    }

}
