package edu.media;
import jakarta.persistence.*;
import java.time.LocalDateTime;

class Entity {
 @jakarta.persistence.Entity static class Dataset {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public String name; public String description; public String path; public long sizeBytes; public int samples; public String status="未使用"; public LocalDateTime createdAt=LocalDateTime.now();
 }
 @jakarta.persistence.Entity static class Training {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long datasetId; public String name; public String algorithm; public String target; public String features; public String params; public String status="待训练"; public String modelPath; public double mae; public double mse; public double r2; public String error; public LocalDateTime createdAt=LocalDateTime.now(); public LocalDateTime finishedAt;
  @Column(length=2000000) public String reportJson;
 }
 @jakarta.persistence.Entity static class Video {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public String title; public String author; public String category; public LocalDateTime publishedAt; public double duration; public long views; public long likes; public long comments; public long shares; public long favorites; public int hashtags; public boolean hotBgm; public String publishPeriod; public Double predictedViews; public Double predictedScore; public String heatLevel;
 }
 @jakarta.persistence.Entity static class Prediction {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long modelId; public double value; public double score; public String level; public LocalDateTime createdAt=LocalDateTime.now();
 }
}
