package org.wangpai.demo.lock;

import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 为了避免反复新建线程的开销，此类会事先就后台运行，然后供所有的线程共用
 *
 * @since 2022-3-19
 */
@Accessors(chain = true)
public class LockRenewal implements Runnable {
    @Setter
    private static RedisTemplate<String, String> redisTemplate;

    @Setter
    private Thread runningThread;

    /**
     * 锁的名称
     *
     * @since 2022-3-19
     */
    @Setter
    private volatile String lockKey;

    /**
     * 控制线程的启动与终止
     *
     * @since 2022-3-19
     */
    @Setter
    private volatile boolean started = false;

    /**
     * 控制续时任务的暂停与恢复
     *
     * @since 2022-3-19
     */
    private volatile boolean isRunning = false;

    /**
     * 当系统的总线程数过高时，将此字段置位。此时当 isSuspended 也为 true 时，销毁本线程，而不是静默执行空任务
     *
     * @since 2022-3-19
     */
    @Setter
    private volatile boolean fastClosed = false;

    /**
     * 控制续时任务执行间隔时间，单位：秒
     *
     * 注意：timeWaiting 值不能大于 timeRenewal 值。建议 timeWaiting 为 timeRenewal 的 1/3。
     * timeWaiting 与 timeRenewal 过于接近容易导致碰巧因启动时间差，而使续时任务正处于休眠状态而没有及时续时
     *
     * @since 2022-3-19
     */
    @Setter
    private volatile long timeWaiting = 20;

    /**
     * 控制续时时长，单位：秒。
     *
     * @since 2022-3-19
     */
    @Setter
    private volatile long timeRenewal = 60;

    private int count = 0;


    @Override
    public void run() {
        System.out.println("续时线程启动");

        while (this.started) {
            try {
                if (this.fastClosed && !this.isRunning) {
                    return;
                }

                // 第一步应该先休眠，而不应该马上续时
                try {
                    Thread.sleep(this.timeWaiting * 1000);
                } catch (InterruptedException interruptedException) {
                    // 续时任务被外部中断时，线程不退出
                    this.afterInterrupt();
                    continue; // 中断后应该重新开始
                }

                this.count++;
                if (this.isRunning) {
                    this.renewDistributedLock();
                }

            } catch (Throwable throwable) {
                // 此 catch 块是为了避免中途某代码引发异常而导致此线程意外中止
                throwable.printStackTrace();
            }
        }

        System.out.println("续时线程终止");
    }

    /**
     * 此方法必须中断续时任务的休眠
     *
     * @since 2022-3-19
     */
    public void resume() {
        this.isRunning = true;
        this.count = 0;
        this.runningThread.interrupt();
    }

    /**
     * 此方法必须中断续时任务的休眠
     *
     * @since 2022-3-19
     */
    public void suspend() {
        this.isRunning = false;
        this.count = 0;
        this.runningThread.interrupt();
    }

    private void afterInterrupt() {
        this.runningThread.isInterrupted(); // 清除中断标志
        System.out.println("续时任务休眠中断，计数重置");
    }

    private void renewDistributedLock() {
        redisTemplate.expire(this.lockKey, this.timeRenewal, TimeUnit.SECONDS);
        System.out.println("第" + this.count + "次续时成功");
    }
}
