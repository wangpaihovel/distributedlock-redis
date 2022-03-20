package org.wangpai.demo.lock;

import java.util.concurrent.ConcurrentHashMap;
import lombok.Setter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @since 2022-3-13
 */
@Component
public class DistributedLockFactory {
    private static ConcurrentHashMap<String, LockRenewal> asynchronousRenewals = new ConcurrentHashMap<>();

    private static LockTypeRegister lockTypeRegister;

    @Setter
    private static volatile int threadLimit = 100;

    public DistributedLockFactory(RedisTemplate<String, String> redisTemplate, LockTypeRegister register) {
        DistributedReentrantLock.setRedisTemplate(redisTemplate);
        LockRenewal.setRedisTemplate(redisTemplate);

        lockTypeRegister = register;
    }

    public static DistributedReentrantLock getDistributedLock(LockType lockType, String originKey) {
        var lockKey = LockTypeUtil.keyCompound(lockType, originKey);

        // 双重检查锁定：第一重判断
        if (!asynchronousRenewals.containsKey(lockKey)) {
            var lock = lockTypeRegister.getRegister().get(lockType);
            try {
                lock.lock(); // 对 lockType 上锁
                // 双重检查锁定：第二重判断
                if (!asynchronousRenewals.containsKey(lockKey)) {
                    var timeRenewal = new LockRenewal();
                    timeRenewal.setLockKey(lockKey).setStarted(true);
                    // 当总线程数达到上限时，设置 timeRenewal 快速销毁
                    if (asynchronousRenewals.entrySet().size() >= threadLimit) {
                        timeRenewal.setFastClosed(true);
                    }
                    asynchronousRenewals.put(lockKey, timeRenewal);
                    var renewalThead = new Thread(timeRenewal);
                    timeRenewal.setRunningThread(renewalThead);
                    renewalThead.start();
                }
            } finally {
                lock.unlock();
            }
        }

        return new DistributedReentrantLock(lockKey);
    }

    public static LockRenewal getRenewal(String name) {
        return asynchronousRenewals.get(name);
    }
}