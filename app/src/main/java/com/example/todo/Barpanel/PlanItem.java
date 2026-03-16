package com.example.todo.Barpanel;

import java.util.Calendar;

public class PlanItem {

    public static final int TYPE_DAY      = 0;
    public static final int TYPE_SEPARATOR = 1;

    public final int type;

    public final Calendar day;
    public final int dowIndex;

    public final String separatorText;
    public final boolean isYear;

    public PlanItem(Calendar day, int dowIndex) {
        this.type          = TYPE_DAY;
        this.day           = day;
        this.dowIndex      = dowIndex;
        this.separatorText = null;
        this.isYear        = false;
    }

    public PlanItem(String separatorText, boolean isYear) {
        this.type          = TYPE_SEPARATOR;
        this.separatorText = separatorText;
        this.isYear        = isYear;
        this.day           = null;
        this.dowIndex      = -1;
    }
}