package org.wangpai.demo.lock;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

/**
 * @since 2022-3-21
 */
@Getter
@ToString
@Accessors(chain = true)
@Component
public class LockTypeRegister {
    private final HashMap<LockType, ReentrantLock> register = new HashMap<>();

    public LockTypeRegister() {
        for (LockType lockType : LockType.values()) {
            this.register.put(lockType, new ReentrantLock());
        }
    }
}
