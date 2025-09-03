package com.example.demo.dto;

public class StudentScoreInfo {
    private int examsTaken;
    private Double averageScore;
    private Double bestScore;
    
    public StudentScoreInfo() {}
    
    public int getExamsTaken() {
        return examsTaken;
    }
    
    public void setExamsTaken(int examsTaken) {
        this.examsTaken = examsTaken;
    }
    
    public Double getAverageScore() {
        return averageScore;
    }
    
    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }
    
    public Double getBestScore() {
        return bestScore;
    }
    
    public void setBestScore(Double bestScore) {
        this.bestScore = bestScore;
    }
}
