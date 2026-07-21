package com.ereke.qadam2048;

public class UserScore {
    public String username;
    public long score;
    public String userId;
    public String country; // ЭТОГО ПОЛЯ НЕ ХВАТАЛО

    // Пустой конструктор нужен для Firebase
    public UserScore() {
    }

    public UserScore(String username, long score, String userId) {
        this.username = username;
        this.score = score;
        this.userId = userId;
    }


    // Полный конструктор (опционально)
    public UserScore(String username, long score, String userId, String country) {
        this.username = username;
        this.score = score;
        this.userId = userId;
        this.country = country;
    }
}