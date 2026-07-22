package com.oitapp.api.models;

public class MAREntry {
    private int id;
    private String administeredAt;
    private String outcome;
    private String notes;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAdministeredAt() {
        return administeredAt;
    }

    public void setAdministeredAt(String administeredAt) {
        this.administeredAt = administeredAt;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}