package com.oitapp.api.models;

public class Patient {
    private int id;
    private String firstName;
    private String lastName;
    private String mrn;
    private String dob;
    private String allergen;
    private String currentDose;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getAllergen() {
        return allergen;
    }

    public void setAllergen(String allergen) {
        this.allergen = allergen;
    }

    public String getCurrentDose() {
        return currentDose;
    }

    public void setCurrentDose(String currentDose) {
        this.currentDose = currentDose;
    }

}