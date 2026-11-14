package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.TravelDocumentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private static final int MAX_TOP_K = 20;

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    private final Map<String, String> destinationByDocumentId = new ConcurrentHashMap<>();

    public IngestionService(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    public int ingestDocuments(List<TravelDocumentDto> items, String defaultDestination) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        List<Document> documents = new ArrayList<>();

        for (TravelDocumentDto item : items) {
            if (item == null) continue;
            String dest = hasText(item.destination()) ? item.destination().trim()
                    : (hasText(defaultDestination) ? defaultDestination.trim() : "General");
            documents.add(toDocument(item, dest));
        }

        return recordAndStore(documents);
    }

    public int ingestDestinationKnowledge(List<String> rawArticles, String destination) {
        if (rawArticles == null || rawArticles.isEmpty()) {
            return 0;
        }

        String dest = hasText(destination) ? sanitizeText(destination).trim() : "General";
        List<Document> docs = rawArticles.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeText)
                .filter(this::hasText)
                .map(String::trim)
                .map(content -> new Document(
                        generateDeterministicId(dest, "article", content),
                        content,
                        Map.of("destination", dest, "type", "travel_guide")
                ))
                .toList();

        return recordAndStore(docs);
    }

    public int ingestFile(MultipartFile file, String fallbackDestination) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String dest = hasText(fallbackDestination) ? sanitizeText(fallbackDestination).trim() : "General";

        if (filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes(), file.getOriginalFilename());
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder().build());
            List<Document> rawDocs = reader.get().stream()
                    .map(this::sanitizeDocument)
                    .filter(doc -> doc != null && hasText(doc.getText()))
                    .toList();
            List<Document> splitDocs = TokenTextSplitter.builder().build().apply(rawDocs);

            String cleanFilename = sanitizeText(file.getOriginalFilename());
            List<Document> enrichedDocs = splitDocs.stream()
                    .map(this::sanitizeDocument)
                    .filter(doc -> doc != null && hasText(doc.getText()))
                    .map(doc -> {
                        Map<String, Object> meta = new HashMap<>(doc.getMetadata());
                        meta.put("destination", dest);
                        meta.put("type", "travel_guide");
                        if (cleanFilename != null) {
                            meta.put("source_filename", cleanFilename);
                        }
                        String trimmedText = doc.getText().trim();
                        String sourceHeader = cleanFilename != null ? "\nSource: " + cleanFilename : "";
                        String content = "Destination: " + dest + sourceHeader + "\n\n" + trimmedText;
                        String docId = generateDeterministicId(dest, cleanFilename, trimmedText);
                        return new Document(docId, content, meta);
                    })
                    .toList();

            return recordAndStore(enrichedDocs);
        }

        if (filename.endsWith(".json") || (file.getContentType() != null && file.getContentType().contains("json"))) {
            JsonNode root;
            try (InputStream is = file.getInputStream()) {
                root = objectMapper.readTree(is);
            }

            if (root.isArray()) {
                if (!root.isEmpty() && root.get(0).isTextual()) {
                    List<String> articles = objectMapper.convertValue(root, new TypeReference<List<String>>() {});
                    return ingestDestinationKnowledge(articles, dest);
                }
                List<TravelDocumentDto> items = objectMapper.convertValue(root, new TypeReference<List<TravelDocumentDto>>() {});
                return ingestDocuments(items, dest);
            }

            if (root.isObject()) {
                String targetDest = root.hasNonNull("destination") ? root.get("destination").asText() : dest;
                if (root.has("documents")) {
                    List<TravelDocumentDto> items = objectMapper.convertValue(root.get("documents"), new TypeReference<List<TravelDocumentDto>>() {});
                    return ingestDocuments(items, targetDest);
                }
                if (root.has("articles")) {
                    List<String> articles = objectMapper.convertValue(root.get("articles"), new TypeReference<List<String>>() {});
                    return ingestDestinationKnowledge(articles, targetDest);
                }
                return ingestDocuments(List.of(objectMapper.convertValue(root, TravelDocumentDto.class)), targetDest);
            }

            throw new IllegalArgumentException("Unsupported JSON structure for ingestion.");
        }

        if (!filename.endsWith(".txt") && !filename.endsWith(".md")) {
            throw new IllegalArgumentException("Unsupported file type. Use .pdf, .json, .txt or .md.");
        }

        String rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
        String text = sanitizeText(rawText);
        List<String> articles = Arrays.stream(text.split("(?m)^\\s*$\\n+"))
                .map(String::trim)
                .filter(this::hasText)
                .toList();

        return ingestDestinationKnowledge(articles.isEmpty() ? List.of(text.trim()) : articles, dest);
    }

    public List<Document> similaritySearch(String query, int topK) {
        String cleanQuery = sanitizeText(query);
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(cleanQuery != null ? cleanQuery : "").topK(Math.min(MAX_TOP_K, Math.max(1, topK))).build()
        );
    }

    public int getIngestedDocumentCount() {
        return destinationByDocumentId.size();
    }

    public Set<String> getIngestedDestinations() {
        return new TreeSet<>(destinationByDocumentId.values());
    }

    public Map<String, Integer> getDestinationDocumentCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        destinationByDocumentId.values().forEach(dest -> counts.merge(dest, 1, Integer::sum));
        return counts;
    }

    public void restoreDocumentIndex(Map<String, String> destinationById) {
        destinationById.forEach((id, dest) -> destinationByDocumentId.putIfAbsent(id, hasText(dest) ? dest.trim() : "General"));
    }

    private Document toDocument(TravelDocumentDto item, String destination) {
        String dest = sanitizeText(destination);
        dest = hasText(dest) ? dest.trim() : "General";
        String content = (item.content() != null) ? sanitizeText(item.content()).trim() : "";
        String title = (item.title() != null) ? sanitizeText(item.title()).trim() : "";
        String docId = hasText(item.id())
                ? sanitizeText(item.id()).trim()
                : generateDeterministicId(dest, title, content);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("destination", dest);
        metadata.put("doc_id", docId);
        metadata.put("type", "travel_guide");

        putIfText(metadata, "title", sanitizeText(item.title()));
        putIfText(metadata, "category", sanitizeText(item.category()));
        putIfText(metadata, "district", sanitizeText(item.district()));
        putIfText(metadata, "best_time_to_visit", sanitizeText(item.bestTimeToVisit()));
        putIfText(metadata, "suggested_duration", sanitizeText(item.suggestedDuration()));

        if (item.tags() != null && !item.tags().isEmpty()) {
            List<String> cleanTags = item.tags().stream()
                    .filter(Objects::nonNull)
                    .map(this::sanitizeText)
                    .filter(this::hasText)
                    .toList();
            if (!cleanTags.isEmpty()) {
                metadata.put("tags", String.join(", ", cleanTags));
            }
        }
        if (item.metadata() != null && !item.metadata().isEmpty()) {
            metadata.putAll(sanitizeMetadata(item.metadata()));
        }

        StringBuilder sb = new StringBuilder("Destination: ").append(dest).append("\n");
        appendField(sb, "Title: ", sanitizeText(item.title()));
        appendField(sb, "District: ", sanitizeText(item.district()));
        appendField(sb, "Category: ", sanitizeText(item.category()));
        appendField(sb, "Best Time to Visit: ", sanitizeText(item.bestTimeToVisit()));
        appendField(sb, "Suggested Duration: ", sanitizeText(item.suggestedDuration()));

        if (hasText(content)) {
            sb.append("\n").append(content);
        }

        return new Document(docId, sb.toString().trim(), metadata);
    }

    private int recordAndStore(List<Document> docs) {
        if (docs == null || docs.isEmpty()) return 0;

        Map<String, Document> uniqueDocsMap = new LinkedHashMap<>();
        for (Document rawDoc : docs) {
            if (rawDoc == null) continue;
            Document doc = sanitizeDocument(rawDoc);
            if (doc != null && hasText(doc.getId()) && hasText(doc.getText())) {
                uniqueDocsMap.put(doc.getId(), doc);
            }
        }

        if (uniqueDocsMap.isEmpty()) return 0;
        List<Document> uniqueDocs = new ArrayList<>(uniqueDocsMap.values());

        vectorStore.add(uniqueDocs);

        Set<String> destinations = new TreeSet<>();
        for (Document doc : uniqueDocs) {
            Object destMeta = doc.getMetadata().get("destination");
            String dest = (destMeta instanceof String d && hasText(d)) ? d.trim() : "General";
            destinationByDocumentId.put(doc.getId(), dest);
            destinations.add(dest);
        }

        log.info("Successfully ingested {} unique documents into VectorStore for destinations: {}", uniqueDocs.size(), destinations);
        return uniqueDocs.size();
    }

    private Document sanitizeDocument(Document doc) {
        if (doc == null) {
            return null;
        }
        String cleanId = sanitizeText(doc.getId());
        String cleanText = sanitizeText(doc.getText());
        Map<String, Object> cleanMetadata = sanitizeMetadata(doc.getMetadata());
        return new Document(cleanId, cleanText, cleanMetadata);
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> clean = new HashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() == null) continue;
            String cleanKey = sanitizeText(entry.getKey());
            if (!hasText(cleanKey)) continue;
            clean.put(cleanKey, sanitizeValue(entry.getValue()));
        }
        return clean;
    }

    private Object sanitizeValue(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof String s) {
            return sanitizeText(s);
        } else if (val instanceof List<?> list) {
            return list.stream()
                    .map(this::sanitizeValue)
                    .toList();
        } else if (val instanceof Map<?, ?> map) {
            Map<String, Object> cleanSubMap = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) continue;
                String k = sanitizeText(e.getKey().toString());
                if (hasText(k)) {
                    cleanSubMap.put(k, sanitizeValue(e.getValue()));
                }
            }
            return cleanSubMap;
        }
        return val;
    }

    private String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\u0000", "");
    }

    private String generateDeterministicId(String destination, String prefixOrTitle, String content) {
        String rawKey = (destination != null ? destination.trim().toLowerCase() : "") + "::"
                + (prefixOrTitle != null ? prefixOrTitle.trim().toLowerCase() : "") + "::"
                + (content != null ? content.trim().toLowerCase() : "");
        return UUID.nameUUIDFromBytes(rawKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    private void putIfText(Map<String, Object> map, String key, String val) {
        if (hasText(val)) map.put(key, val.trim());
    }

    private void appendField(StringBuilder sb, String label, String val) {
        if (hasText(val)) sb.append(label).append(val.trim()).append("\n");
    }
}

