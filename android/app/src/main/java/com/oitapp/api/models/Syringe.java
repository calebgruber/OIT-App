package com.oitapp.api.models;

public class Syringe {
    private int id;
    private String barcode;
    private String doseAmount;
    private String doseForm;
    private String status;
    private String expiryAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getDoseAmount() {
        return doseAmount;
    }

    public void setDoseAmount(String doseAmount) {
        this.doseAmount = doseAmount;
    }

    public String getDoseForm() {
        return doseForm;
    }

    public void setDoseForm(String doseForm) {
        this.doseForm = doseForm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpiryAt() {
        return expiryAt;
    }

    public void setExpiryAt(String expiryAt) {
        this.expiryAt = expiryAt;
    }

}