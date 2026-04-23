package com.example.enrollment.domain.course;

public enum CourseStatus {
    DRAFT("초안"),
    OPEN("모집 중"),
    CLOSED("모집 마감");

    private final String description;

    CourseStatus(String description){
        this.description = description;
    }

    String getDescription(){
        return description;
    }
}
