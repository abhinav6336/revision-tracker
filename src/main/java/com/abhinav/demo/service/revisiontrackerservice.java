package com.abhinav.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.abhinav.demo.model.Revision;
import com.abhinav.demo.repo.revisionrepo;

import java.time.LocalDate;
import java.util.List;
@Service
public class revisiontrackerservice {
    @Autowired
    revisionrepo repo;
    public void addtopics(Revision s){
        int [] arr ={1,3,7,14,30,90,180,365};
        LocalDate today=LocalDate.now();
        StringBuilder str = new StringBuilder();
        for(int n : arr){
        LocalDate future=today.plusDays(n);
        str.append(future.toString());
        str.append("\n");
        }
        s.setautodates(str.toString());
        s.setdate(LocalDate.now().toString());
        long temp=repo.count();
      //  s.setposition(temp+1);
        s.setid(temp+1);
        repo.save(s);
    }
  //  public void removetopics(long id){
  //      Revision todelete = repo.findById(id).orElseThrow(() -> new RuntimeException("No topic found"));
  //      long deletepos=todelete.getposition();
  //      repo.deleteById(id);
  //      List<Revision> all = repo.findAll();
   //     for(Revision r : all){
   //         if(r.getposition() > deletepos){
   //             r.setposition(r.getposition()-1);
   //         }
  //      }
  //      repo.saveAll(all);
  //  }
    public List<Revision> gettopics(){
          return repo.findAll();
    }
}
