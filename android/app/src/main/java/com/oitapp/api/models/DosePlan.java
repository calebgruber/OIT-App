package com.oitapp.api.models;

public class DosePlan {
    private int id;
    private String currentDose;
    private String nextDose;
    private String nextUpdoseDate;
    private String allergen;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCurrentDose() {
        return currentDose;
    }

    public void setCurrentDose(String currentDose) {
        this.currentDose = currentDose;
    }

    public String getNextDose() {
        return nextDose;
    }

    public void setNextDose(String nextDose) {
        this.nextDose = nextDose;
    }

    public String getNextUpdoseDate() {
        return nextUpdoseDate;
    }

    public void setNextUpdoseDate(String nextUpdoseDate) {
        this.nextUpdoseDate = nextUpdoseDate;
    }

    public String getAllergen() {
        return allergen;
    }

    public void setAllergen(String allergen) {
        this.allergen = allergen;
    }

}