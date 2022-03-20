package org.wangpai.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wangpai.demo.lock.DistributedLockFactory;
import org.wangpai.demo.lock.LockType;

/**
 * @since 2022-3-20
 */
@Service
public class DemoService {
    @Transactional
    public DemoService demoService() {
        final var spinTime = 1; // 自旋时间，单位：秒
        var someKey = "someKey";
        var lock = DistributedLockFactory.getDistributedLock(LockType.LOCK_1, someKey);
        try {
            int count = 0;
            // 获取分布式锁
            while (!lock.tryLock()) {
                try {
                    Thread.sleep(spinTime * 1000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }

                // TODO：判断现在是否已经不需要得到锁了。如果是，退出此自旋

                System.out.println("第" + (++count) + "次没有拿到锁，尝试下一次");
            }
            System.out.println("得到分布式锁");

            // TODO：判断现在是否已经不需要得到锁了。如果是，直接放弃锁

            System.out.println("得到分布式锁，但可能已经不需要了"); // TODO：需要将此日志更正为更具体的日志信息

            // TODO：业务代码

        } finally {
            System.out.println("尝试释放分布式锁");
            // 无论前面是否抛出异常，此处都要释放锁。这不会释放别人的锁
            lock.unlock();
        }

        // TODO：不需要上锁的业务代码

        return this;
    }

}
