package ru.itmo.soa.city.model;

import java.util.Date;

public class Human {
    private Date birthday;

    public Human(Date birthday) {
        this.birthday = birthday;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
