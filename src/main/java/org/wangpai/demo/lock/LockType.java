package org.wangpai.demo.lock;

import lombok.Getter;

/**
 * @since 2022-2-24
 */
public enum LockType {
    LOCK_1("lock_1"), // TODO：需要将此枚举改为更具体的名称
    ;

    @Getter
    private String name;

    LockType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
