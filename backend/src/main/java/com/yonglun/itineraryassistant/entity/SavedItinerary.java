package com.yonglun.itineraryassistant.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saved_itineraries")
public class SavedItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "destination")
    private String destination;

    @Column(name = "days_count")
    private Integer daysCount;

    @Column(name = "form_data_json", columnDefinition = "TEXT")
    private String formDataJson;

    @Column(name = "itinerary_json", columnDefinition = "TEXT")
    private String itineraryJson;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "created_at")
    private Instant createdAt;

    public SavedItinerary() {}

    public SavedItinerary(Long id, String conversationId, String destination, Integer daysCount,
                          String formDataJson, String itineraryJson, String rawText, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.destination = destination;
        this.daysCount = daysCount;
        this.formDataJson = formDataJson;
        this.itineraryJson = itineraryJson;
        this.rawText = rawText;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDaysCount() {
        return daysCount;
    }

    public void setDaysCount(Integer daysCount) {
        this.daysCount = daysCount;
    }

    public String getFormDataJson() {
        return formDataJson;
    }

    public void setFormDataJson(String formDataJson) {
        this.formDataJson = formDataJson;
    }

    public String getItineraryJson() {
        return itineraryJson;
    }

    public void setItineraryJson(String itineraryJson) {
        this.itineraryJson = itineraryJson;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
