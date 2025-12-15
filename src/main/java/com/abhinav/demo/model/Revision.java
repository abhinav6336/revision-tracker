package com.abhinav.demo.model;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "revisions")
public class Revision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    private String topic;
    private String date;
    private String revisiondates;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    public Revision(){}
    
    public Revision(String topic, String date, String revisiondates, User user){
        this.topic = topic;
        this.date = LocalDate.now().toString();
        this.revisiondates = revisiondates;
        this.user = user;
    }
    
    public long getId(){
        return id;
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public String getTopic(){
        return topic;
    }
    
    public void setTopic(String topic){
        this.topic = topic;
    }
    
    public String getDate(){
        return date;
    }
    
    public void setDate(String date){
        this.date = date;
    }
    
    public String getAutodates(){
        return revisiondates;
    }
    
    public void setAutodates(String revisiondates){
        this.revisiondates = revisiondates;
    }
    
    public User getUser(){
        return user;
    }
    
    public void setUser(User user){
        this.user = user;
    }
    
    @Override
    public String toString() {
        return "Revision{" +
                "id=" + id +
                ", topic='" + topic + '\'' +
                ", date='" + date + '\'' +
                ", revisiondates='" + revisiondates + '\'' +
                ", userId=" + (user != null ? user.getId() : null) +
                '}';
    }
}
