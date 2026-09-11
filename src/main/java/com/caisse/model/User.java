package com.caisse.model;

public class User {
    private String id;         // Supabase auth uuid
    private String email;
    private String cashierCode; // e.g. USER001

    public User(String id, String email, String cashierCode) {
        this.id = id;
        this.email = email;
        this.cashierCode = cashierCode;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getCashierCode() { return cashierCode; }
}
