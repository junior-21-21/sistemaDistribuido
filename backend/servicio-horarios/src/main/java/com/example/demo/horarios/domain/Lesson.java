package com.example.demo.horarios.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;

@PlanningEntity
@Entity
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String subject;
    private String teacher;
    private String studentGroup;

    @PlanningVariable
    @ManyToOne
    private Timeslot timeslot;

    @PlanningVariable
    @ManyToOne
    private Room room;

    public Lesson() {}

    public Lesson(String subject, String teacher, String studentGroup) {
        this.subject = subject;
        this.teacher = teacher;
        this.studentGroup = studentGroup;
    }

    @PlanningId
    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getTeacher() { return teacher; }
    public String getStudentGroup() { return studentGroup; }
    public Timeslot getTimeslot() { return timeslot; }
    public void setTimeslot(Timeslot timeslot) { this.timeslot = timeslot; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    @Override
    public String toString() {
        return subject + "(" + id + ")";
    }
}
