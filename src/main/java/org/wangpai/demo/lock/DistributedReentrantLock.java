package org.wangpai.demo.lock;

import java.util.concurrent.TimeUnit;
import lombok.Setter;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 分布式可重入锁
 *
 * @since 2022-3-13
 */
public class DistributedReentrantLock {
    @Setter
    private static RedisTemplate<String, String> redisTemplate;

    private final String name;

    /**
     * 线程级可重入
     *
     * @since 2022-3-13
     */
    private final ThreadLocal<Integer> lockedTimes = new ThreadLocal<>();

    @Setter
    private int lockedDuration = 10;

    private TimeUnit lockedDurationUnit = TimeUnit.SECONDS;

    public DistributedReentrantLock(String name) {
        this.name = name;
        this.lockedTimes.set(0);
    }

    /**
     * 尝试加锁，如果失败，返回 false
     *
     * @since 2022-3-13
     */
    public boolean tryLock(long timeout, TimeUnit unit) {
        var times = this.lockedTimes.get();
        boolean isSuccessful = true;

        if (times == 0) {
            isSuccessful = redisTemplate.opsForValue().setIfAbsent(this.name, this.name, timeout, unit);
        }

        if (isSuccessful) {
            this.lockedTimes.set(times + 1);

            var renewal = DistributedLockFactory.getRenewal(this.name);
            renewal.setTimeRenewal(this.lockedDuration)
                    .setTimeWaiting(this.lockedDuration / 2)
                    .resume();
        }

        return isSuccessful;
    }

    /**
     * 尝试最多持续 60s 的锁
     *
     * @since 2022-3-13
     */
    public boolean tryLock() {
        return this.tryLock(this.lockedDuration, this.lockedDurationUnit);
    }

    /**
     * 尝试加锁，如果失败，返回 false
     *
     * @since 2022-3-13
     */
    public boolean tryLock(long timeout) {
        return this.tryLock(timeout, this.lockedDurationUnit);
    }

    /**
     * 只有本线程上过锁时，调用此方法才有效
     *
     * @since 2022-3-13
     */
    public void unlock() {
        var times = this.lockedTimes.get();

        if (times == 0) {
            System.out.println("本线程没有上过锁，解锁失败");
            return;
        }

        // 本线程是否上过锁
        if (times == 1) {
            /**
             * 因为这个锁是互斥锁，所以只要本线程加锁过，其它线程不可能可以加锁，
             * 因此这锁一定是本线程加的，故无需验证线程 id
             */
            redisTemplate.delete(this.name);

            var renewal = DistributedLockFactory.getRenewal(this.name);
            renewal.suspend();

            System.out.println("完全释放分布式锁");
        }
        this.lockedTimes.set(times - 1);
    }
}
