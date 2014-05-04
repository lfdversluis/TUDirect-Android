package com.lfdversluis.tudirect.objects;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CourseWrap implements Comparable<CourseWrap> {


    public boolean sufficient;
    private String courseId;
    private String grade;
    private String mutationDate;
    private String ects;

    public CourseWrap(boolean suf, String prop1, String prop2, String prop3, String prop4) {
        this.courseId = prop1;
        this.grade = prop2;
        this.mutationDate = prop3;
        this.sufficient = suf;
        this.ects = prop4;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getGrade() {
        return grade;
    }

    public String getMutationDate() {
        return mutationDate;
    }

    public String getECTS() {
        return ects;
    }

    public boolean isSufficient() {
        return sufficient;
    }

    @Override
    public int compareTo(CourseWrap o) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
        try {
            Date date1 = sdf.parse(this.mutationDate);
            Date date2 = sdf.parse(o.getMutationDate());

            if (date1 == null || date2 == null) {
                return 0;
            }
            if (date1.after(date2)) return -1;
            else if (date1.before(date2)) return +1;
            else return 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}