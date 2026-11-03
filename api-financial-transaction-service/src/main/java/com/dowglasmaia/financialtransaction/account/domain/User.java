package com.dowglasmaia.financialtransaction.account.domain;

/** The account holder. {@code externalSubject} is the identity provider's subject (token "sub"); unique. */
public final class User {

    private final String id;
    private final String name;
    private final String cpf;
    private final String email;
    private final Integer areaCode;
    private final Integer phoneNumber;
    private final String externalSubject;

    private User(String id, String name, String cpf, String email, Integer areaCode, Integer phoneNumber, String externalSubject) {
        this.id = id;
        this.name = name;
        this.cpf = cpf;
        this.email = email;
        this.areaCode = areaCode;
        this.phoneNumber = phoneNumber;
        this.externalSubject = externalSubject;
    }

    public static User create(String name, String cpf, String email, Integer areaCode, Integer phoneNumber) {
        return new User(null, name, cpf, email, areaCode, phoneNumber, null);
    }

    public static User restore(String id, String name, String cpf, String email, Integer areaCode, Integer phoneNumber,
                               String externalSubject) {
        return new User(id, name, cpf, email, areaCode, phoneNumber, externalSubject);
    }

    /** A copy of this user linked to the given identity-provider subject (also when it was linked to another one). */
    public User linkedTo(String subject) {
        return new User(id, name, cpf, email, areaCode, phoneNumber, subject);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAreaCode() {
        return areaCode;
    }

    public Integer getPhoneNumber() {
        return phoneNumber;
    }

    public String getExternalSubject() {
        return externalSubject;
    }
}
