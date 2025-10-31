package com.example.myapplication.data.Models;

import java.util.HashMap;
import java.util.Map;

public class Users {
    // User profile fields
    private String profilePic, userName, mail, password, userId, lastMessage;
    
    // Game related fields
    private int xp = 0;
    private int level = 1;
    private int totalWins = 0;
    private int currentStreak = 0;
    private int highestStreak = 0;
    private long lastPlayed = 0;
    private Map<String, Integer> gameStats = new HashMap<>();
    
    // Game specific stats
    private int guessTheNumberWins = 0;
    private int rockPaperScissorsWins = 0;
    private int evenOrOddWins = 0;
    private int ticTacToeWins = 0;
    private int mathQuizHighScore = 0;
    private int higherLowerWins = 0;

    public Users(String profilePic, String userName, String mail, String password, String userId, String lastMessage) {
        this.profilePic = profilePic;
        this.userName = userName;
        this.mail = mail;
        this.password = password;
        this.userId = userId;
        this.lastMessage = lastMessage;
        initializeGameStats();
    }

    public Users() {
        initializeGameStats();
    }

    // SignUp

    public Users(String userName, String mail, String password) {
        this.userName = userName;
        this.mail = mail;
        this.password = password;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userMame) {
        this.userName = userMame;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    // Game related getters and setters
    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
        updateLevel();
    }
    
    public void addXp(int amount) {
        this.xp += amount;
        updateLevel();
    }
    
    public int getLevel() {
        return level;
    }
    
    private void updateLevel() {
        this.level = (int) Math.floor(Math.sqrt(xp / 100)) + 1;
    }
    
    public int getTotalWins() {
        return totalWins;
    }
    
    public void incrementWins() {
        this.totalWins++;
        this.currentStreak++;
        if (currentStreak > highestStreak) {
            highestStreak = currentStreak;
        }
    }
    
    public void resetStreak() {
        this.currentStreak = 0;
    }
    
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    public int getHighestStreak() {
        return highestStreak;
    }
    
    public long getLastPlayed() {
        return lastPlayed;
    }
    
    public void updateLastPlayed() {
        this.lastPlayed = System.currentTimeMillis();
    }
    
    public Map<String, Integer> getGameStats() {
        return gameStats;
    }
    
    public void updateGameStat(String gameId, int score) {
        gameStats.put(gameId, score);
    }
    
    public int getGameStat(String gameId) {
        return gameStats.getOrDefault(gameId, 0);
    }
    
    // Game specific getters and setters
    public int getGuessTheNumberWins() {
        return guessTheNumberWins;
    }
    
    public void incrementGuessTheNumberWins() {
        this.guessTheNumberWins++;
        incrementWins();
    }
    
    public int getRockPaperScissorsWins() {
        return rockPaperScissorsWins;
    }
    
    public void incrementRockPaperScissorsWins() {
        this.rockPaperScissorsWins++;
        incrementWins();
    }
    
    public int getEvenOrOddWins() {
        return evenOrOddWins;
    }
    
    public void incrementEvenOrOddWins() {
        this.evenOrOddWins++;
        incrementWins();
    }
    
    public int getTicTacToeWins() {
        return ticTacToeWins;
    }
    
    public void incrementTicTacToeWins() {
        this.ticTacToeWins++;
        incrementWins();
    }
    
    public int getMathQuizHighScore() {
        return mathQuizHighScore;
    }
    
    public void updateMathQuizHighScore(int score) {
        if (score > mathQuizHighScore) {
            this.mathQuizHighScore = score;
            if (score > 0) {
                incrementWins();
            }
        }
    }
    
    public int getHigherLowerWins() {
        return higherLowerWins;
    }
    
    public void incrementHigherLowerWins() {
        this.higherLowerWins++;
        incrementWins();
    }
    
    private void initializeGameStats() {
        // Initialize default game stats
        gameStats.put("guess_the_number", 0);
        gameStats.put("rock_paper_scissors", 0);
        gameStats.put("even_or_odd", 0);
        gameStats.put("tic_tac_toe", 0);
        gameStats.put("math_quiz", 0);
        gameStats.put("higher_lower", 0);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
