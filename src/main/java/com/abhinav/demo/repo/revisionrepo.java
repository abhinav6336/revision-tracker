package com.abhinav.demo.repo;
import com.abhinav.demo.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface revisionrepo extends JpaRepository<Revision,String>{

}
