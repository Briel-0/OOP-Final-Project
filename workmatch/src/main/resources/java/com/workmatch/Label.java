package com.workmatch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "labels")
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String labelGroup; // SKILL, PHYSICAL_DEMAND, ACCESSIBILITY, WORK_SETUP, EDUCATION
    private boolean mustHave;
    private Integer rankOrder;

    public Label() {
    }

    public Label(String name, String labelGroup, boolean mustHave, Integer rankOrder) {
        this.name = name;
        this.labelGroup = labelGroup;
        this.mustHave = mustHave;
        this.rankOrder = rankOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLabelGroup() { return labelGroup; }
    public void setLabelGroup(String labelGroup) { this.labelGroup = labelGroup; }

    public boolean isMustHave() { return mustHave; }
    public void setMustHave(boolean mustHave) { this.mustHave = mustHave; }

    public Integer getRankOrder() { return rankOrder; }
    public void setRankOrder(Integer rankOrder) { this.rankOrder = rankOrder; }
}