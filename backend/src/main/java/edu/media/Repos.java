package edu.media;
import org.springframework.data.jpa.repository.JpaRepository;
interface DatasetRepo extends JpaRepository<Entity.Dataset,Long>{}
interface TrainingRepo extends JpaRepository<Entity.Training,Long>{}
interface VideoRepo extends JpaRepository<Entity.Video,Long>{}
interface PredictionRepo extends JpaRepository<Entity.Prediction,Long>{}
