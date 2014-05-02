package com.lfdversluis.tudirect.objects;

public class ScheduleObject {


    private String course;
    private String beginDate;
    private String endDate;
    private String place;

    public ScheduleObject(String course, String begin, String end, String place) {
        this.course = course;
        this.beginDate = begin;
        this.endDate = end;
        this.place = place;
    }

    public String getCourse() {
        return course;
    }

    public String getBeginDate() {
        return beginDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getPlace() {
        return place;
    }
}