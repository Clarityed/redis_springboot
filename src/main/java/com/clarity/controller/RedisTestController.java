package com.clarity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 测试控制层
 *
 * @author: clarity
 * @date: 2022年10月29日 10:48
 */
@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/test")
    public String testRedis() {
        // 设置值到 Redis 里面
        redisTemplate.opsForValue().set("role", "keQing");
        // 从 Redis 中取值
        return (String) redisTemplate.opsForValue().get("role");
    }

    @GetMapping("/testLock")
    public void testLock() {
        // 1. 获取锁，setnx 和 setIfAbsent 相同的作用，一个是 Redis 里的方法，一个是 Java 里的方法。
        // 每个请求都会有自己的 uuid
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        // 2. 获取成功查询 number 的值
        if (lock) {
            // 2.1 判断 number 的值是否为空，如果是空就 return
            Object value = redisTemplate.opsForValue().get("number");
            if (StringUtils.isEmpty(value)) {
                return;
            }
            // 2.2 有值就转成 int
            int number = Integer.parseInt(value + "");
            // 2.3 把 redis 的 number 加 1
            redisTemplate.opsForValue().set("number", ++number);
            // 2.4 释放锁，del，加入判断是否是自己的锁
            if (uuid.equals((String) redisTemplate.opsForValue().get("lock"))) {
                redisTemplate.delete("lock");
            }
        } else {
            // 3. 获取锁失败、每隔 0.1 秒再获取
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/testLockLua")
    public void testLockLua() {
        //1 声明一个uuid ,将做为一个value 放入我们的key所对应的值中
        String uuid = UUID.randomUUID().toString();
        //2 定义一个锁：lua 脚本可以使用同一把锁，来实现删除！
        String skuId = "25"; // 访问 skuId 为25号的商品 100008348542
        String locKey = "lock:" + skuId; // 锁住的是每个商品的数据

        // 3 获取锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(locKey, uuid, 3, TimeUnit.SECONDS);

        // 第一种： lock 与过期时间中间不写任何的代码。
        // redisTemplate.expire("lock",10, TimeUnit.SECONDS);//设置过期时间
        // 如果true
        if (lock) {
            // 执行的业务逻辑开始
            // 获取缓存中的 num 数据
            Object value = redisTemplate.opsForValue().get("number");
            // 如果是空直接返回
            if (StringUtils.isEmpty(value)) {
                return;
            }
            // 不是空 如果说在这出现了异常！ 那么 delete 就删除失败！ 也就是说锁永远存在！
            int number = Integer.parseInt(String.valueOf(value));
            // 使 num 每次 +1 放入缓存
            redisTemplate.opsForValue().set("number", ++number);
            /*使用 lua 脚本来锁*/
            // 定义lua 脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            // 使用 redis 执行 lua 执行
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            // 设置一下返回值类型 为 Long
            // 因为删除判断的时候，返回的 0,给其封装为数据类型。如果不封装那么默认返回 String 类型，
            // 那么返回字符串与 0 会有发生错误。
            redisScript.setResultType(Long.class);
            // 第一个要是 script 脚本 ，第二个需要判断的 key，第三个就是 key 所对应的值。
            redisTemplate.execute(redisScript, Arrays.asList(locKey), uuid);
        } else {
            // 其他线程等待
            try {
                // 睡眠
                Thread.sleep(100);
                // 睡醒了之后，调用方法。
                testLockLua();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
