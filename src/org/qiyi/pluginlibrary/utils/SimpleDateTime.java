package org.qiyi.pluginlibrary.utils;

/**
 * 系统的 Date，替代方案，因为系统Date的初始化性能太慢。
 */
public class SimpleDateTime {
    /** year */
    int mYear;
    /** month */
    int mMonth;
    /** month of the year */
    int mDay;
    /** hour */
    int mHourOfDay;
    /** minute */
    int mMinute;
    /** second */
    int mSecond;
    
    /**
     * 设置date time
     * @param year year
     * @param month month
     * @param day day of month
     * @param hourOfDay hourOfDay
     * @param minute minute
     * @param second second
     */
    public final void set(int year, int month, int day, int hourOfDay, int minute, int second) {
        this.mYear = year;
        this.mMonth = month;
        this.mDay = day;
        this.mHourOfDay = hourOfDay;
        this.mMinute = minute;
        this.mSecond = second;
    }
    
    @Override
    public String toString() {
        return mYear + "-" + mMonth + "-" + mDay + " " + mHourOfDay + ":" + mMinute + ":" + mSecond;
    }
    
    /**
     * 比较两个的日期大小。
     * @param datetime 要比较的对象。
     * @return == 返回0， > 返回1， < 返回-1
     */
    public int compareTo(SimpleDateTime datetime) {

        if (mYear - datetime.mYear > 0) {
            return 1;
        } else if (mYear - datetime.mYear < 0) {
            return -1;
        }
        
        if (mMonth - datetime.mMonth > 0) {
            return 1;
        } else if (mMonth - datetime.mMonth < 0) {
            return -1;
        }
        
        if (mDay - datetime.mDay > 0) {
            return 1;
        } else if (mDay - datetime.mDay < 0) {
            return -1;
        }
        
        if (mHourOfDay - datetime.mHourOfDay > 0) {
            return 1;
        } else if (mHourOfDay - datetime.mHourOfDay < 0) {
            return -1;
        }
        
        if (mMinute - datetime.mMinute > 0) {
            return 1;
        } else if (mMinute - datetime.mMinute < 0) {
            return -1;
        }
        
        if (mSecond - datetime.mSecond > 0) {
            return 1;
        } else if (mSecond - datetime.mSecond < 0) {
            return -1;
        }
        
        return 0;
    }
}
