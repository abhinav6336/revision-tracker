package com.abhinav.demo.model;
import java.time.LocalDate;

import com.abhinav.demo.repo.revisionrepo;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
@Entity
public class Revision {
    @Id
    private String topic;
    private long id;
    private String date;
    private String revisiondates;
    public Revision(){}
    public Revision(String topic,String date,long id,String revisiondates){
        this.topic=topic;
        this.date=LocalDate.now().toString();
        this.id=id;
        this.revisiondates=revisiondates;
    }
  //  public long getposition(){
  //      return position;
  //  }
  //  public void setposition(long position){
  //      this.position=position;
  //  }
    public String getautodates(){
        return revisiondates;
    }
    public void setautodates(String revisiondates){
        this.revisiondates=revisiondates;
    }
    public long getid(){
        return id;
    }
    public void setid(long id){
        this.id=id;
    }
    public String gettopic(){
        return topic;
    }
    public String getdate(){
        return date;
    }
    public void settopic(String topic){
        this.topic=topic;
    }
    public void setdate(String date){
        this.date=date;
    }
}
