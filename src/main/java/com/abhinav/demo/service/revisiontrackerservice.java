package com.abhinav.demo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhinav.demo.model.Revision;
import com.abhinav.demo.repo.revisionrepo;

@Service
public class revisiontrackerservice {
    @Autowired
    revisionrepo repo;

    public void addtopics(Revision s) {
        int[] arr = { 1, 3, 7, 14, 30, 90, 180, 365 };
        LocalDate today = LocalDate.now();
        StringBuilder str = new StringBuilder();
        for (int n : arr) {
            LocalDate future = today.plusDays(n);
            str.append(future.toString());
            str.append("\n");
        }
        s.setAutodates(str.toString());
        s.setDate(LocalDate.now().toString());
        repo.save(s);
    }

    public boolean topicExists(String topic, Long userId) {
        return repo.existsByTopicAndUserId(topic, userId);
    }

    public List<Revision> gettopics(Long userId) {
        // return newest topics first
        return repo.findByUserIdOrderByIdDesc(userId);
    }

    public void removetopics(String topic, Long userId) {
        repo.deleteByTopicAndUserId(topic, userId);
    }

    @Transactional
    public void removetopicsTransactional(String topic, Long userId) {
        repo.deleteByTopicAndUserId(topic, userId);
    }

    public List<String> getTopicsForRevision(Long userId, LocalDate date) {
        List<Revision> all = repo.findByUserId(userId);
        String dateStr = date.toString();
        // Return topics where autodates contains the date string
        // We also want to include topics created TODAY? The prompt said "topics u have
        // to revise today".
        // Typically that includes things scheduled for today.
        return all.stream()
                .filter(r -> (r.getAutodates() != null && r.getAutodates().contains(dateStr))
                        || (r.getDate() != null && r.getDate().equals(dateStr)))
                .map(Revision::getTopic)
                .distinct()
                .toList();
    }
}
