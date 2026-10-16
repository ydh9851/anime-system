package com.alvis.media.domain.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 训练任务状态机：PENDING -> RUNNING -> SUCCESS / FAILED
 */
public enum TrainTaskStatusEnum {

    PENDING(0, "排队中"),
    RUNNING(1, "训练中"),
    SUCCESS(2, "训练成功"),
    FAILED(3, "训练失败");

    int code;
    String name;

    TrainTaskStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    private static final Map<Integer, TrainTaskStatusEnum> keyMap = new HashMap<>();

    static {
        for (TrainTaskStatusEnum item : TrainTaskStatusEnum.values()) {
            keyMap.put(item.getCode(), item);
        }
    }

    public static TrainTaskStatusEnum fromCode(String status) {
        if (status == null) {
            return null;
        }
        for (TrainTaskStatusEnum item : values()) {
            if (item.name().equalsIgnoreCase(status)) {
                return item;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
