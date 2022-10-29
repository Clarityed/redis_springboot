package com.clarity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
