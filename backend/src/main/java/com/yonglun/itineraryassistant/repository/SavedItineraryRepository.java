package com.yonglun.itineraryassistant.repository;

import com.yonglun.itineraryassistant.entity.SavedItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedItineraryRepository extends JpaRepository<SavedItinerary, Long> {
    Optional<SavedItinerary> findFirstByConversationIdOrderByIdDesc(String conversationId);
}
