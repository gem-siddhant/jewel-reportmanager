package com.jewel.reportmanager.enums;

public enum StatusColor {
    PASS("rgb(44, 174, 33,0.7)",5),
    FAIL("rgb(234, 62, 62,0.7)",3),
    WARN("rgb(255, 108, 55,0.7)",4),
    INFO("rgb(73, 132, 163,0.7)",6),
    ERR("rgb(234, 62, 62)",2),
    EXE("rgb(158, 158, 158,0.7)",7),
    PENDING("rgb(158, 158, 158,0.7)",1),
    TOTAL("rgb(158, 158, 158,0.7)",1),

    OTHERS("rgb(158, 158, 158,0.7)",8)
    ;

    public final String color;
    public final int priority;

    public String getColor() {
        return color;
    }

    public int getPriority() {
        return priority;
    }

    private StatusColor(String color, int priority) {
        this.color = color;
        this.priority=priority;
    }
    
}
