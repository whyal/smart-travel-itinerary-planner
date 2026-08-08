package com.yonglun.itineraryassistant.repository;

import com.yonglun.itineraryassistant.entity.SavedItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedItineraryRepository extends JpaRepository<SavedItinerary, Long> {
    List<SavedItinerary> findByConversationIdOrderByCreatedAtDesc(String conversationId);
    List<SavedItinerary> findAllByOrderByCreatedAtDesc();
}
