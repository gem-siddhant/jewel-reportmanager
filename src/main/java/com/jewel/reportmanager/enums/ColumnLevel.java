package com.jewel.reportmanager.enums;

public enum ColumnLevel {

    JOB_NAME(1),
    PROJECT_REPORT(2),
    PROJECT(3),
    FRAMEWORK(4);


    public final int priority;

    public int getPriority() {
        return priority;
    }

    ColumnLevel(int priority) {
        this.priority=priority;
    }
}
