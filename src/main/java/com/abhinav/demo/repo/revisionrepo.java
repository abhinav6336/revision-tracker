package com.abhinav.demo.repo;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.abhinav.demo.model.Revision;

@Repository
public interface revisionrepo extends JpaRepository<Revision, Long>{
    List<Revision> findByUserId(Long userId);
    Optional<Revision> findByTopicAndUserId(String topic, Long userId);
    boolean existsByTopicAndUserId(String topic, Long userId);
    void deleteByTopicAndUserId(String topic, Long userId);
}
