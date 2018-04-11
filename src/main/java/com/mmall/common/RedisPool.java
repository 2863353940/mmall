package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by Administrator on 2018/4/11.
 */
public class RedisPool {

    private static JedisPool pool; // jedis链接池
    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total", "20")); // 最大连接数
    private static Integer maxIdel = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle", "10")); // 在jedispool中最大的idle（空闲）状态的jedis实例的个数
    private static Integer minIdel = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle", "2")); // 在jedispool中最小的idle（空闲）状态的jedis实例的个数
    private static Boolean testOnBorrow = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow", "true")); // 在borrow一个jedis实例的时候，是否要进行验证操作，如果赋值true。则得到的jedis实例肯定是可以用的
    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return", "true")); // 在return一个jedis实例的时候，是否要进行验证操作，如果赋值true。则放回jedispool的jedis实例肯定是可以用的

    private static String redisIp = PropertiesUtil.getProperty("redis.ip");
    private static Integer redisPort = Integer.parseInt(PropertiesUtil.getProperty("redis.port"));

    private static void initPool(){
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdel);
        config.setMinIdle(minIdel);
        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);
        config.setBlockWhenExhausted(true); // 链接耗尽时，是否阻塞，false会发生异常，true阻塞直到超时,默认为true

        pool = new JedisPool(config, redisIp, redisPort, 1000*2);
    }

    static {
        initPool();
    }

    /**
     * 获取连接
     * @return
     */
    public static Jedis getJedis(){
        return pool.getResource();
    }

    /**
     * 返回一个坏的异常的连接
     * @param jedis
     */
    public static void returnBrokenResource(Jedis jedis){
        pool.returnBrokenResource(jedis);
    }

    /**
     * 返回一个正常的连接
     * @param jedis
     */
    public static void returnResource(Jedis jedis){
        pool.returnResource(jedis);
    }


    public static void main(String[] args) {
        Jedis jedis = pool.getResource();
        jedis.set("kkk", "vvv");
        returnResource(jedis);

        pool.destroy(); //临时调用，销毁连接池中的所有连接
    }






}
