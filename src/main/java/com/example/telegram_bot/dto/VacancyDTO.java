package com.example.telegram_bot.dto;

import com.opencsv.bean.CsvBindByName;


public class VacancyDTO {

    @CsvBindByName(column = "Id")
    private String id;
    @CsvBindByName(column = "Title")
    private String title;
    @CsvBindByName(column = "Short description")
    private String shortDescription;
    @CsvBindByName(column = "Long description")
    private String description;
    @CsvBindByName(column = "Company")
    private String company;
    @CsvBindByName(column = "Salary")
    private String salary;
    @CsvBindByName(column = "Link")
    private String link;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
