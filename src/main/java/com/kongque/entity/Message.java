package com.kongque.entity;

public class Message {

    private Integer id;
    private String  theme;
    private String  content;
    private String  official;
    private String  customer;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOfficial() {
        return official;
    }

    public void setOfficial(String official) {
        this.official = official;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }
}
