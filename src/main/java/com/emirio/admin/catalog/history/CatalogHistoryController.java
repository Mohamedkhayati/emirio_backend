package com.emirio.admin.catalog.history;

import com.emirio.admin.CurrentActorService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class CatalogHistoryController {

    private final CatalogHistoryRepository historyRepository;
    private final CurrentActorService currentActorService;

    // Global history - shows ALL catalog activities (articles + variations)
    @GetMapping("/catalog/history/all")
    public List<HistoryDto> getAllHistory(
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String targetType
    ) {
        currentActorService.requireGeneralAdmin();
        
        List<CatalogHistory> histories;
        
        if (action != null && targetType != null) {
            histories = historyRepository.findByActionAndTargetTypeOrderByActionAtDesc(
                CatalogAction.valueOf(action), 
                CatalogTargetType.valueOf(targetType)
            );
        } else if (action != null) {
            histories = historyRepository.findByActionOrderByActionAtDesc(CatalogAction.valueOf(action));
        } else if (targetType != null) {
            histories = historyRepository.findByTargetTypeOrderByActionAtDesc(CatalogTargetType.valueOf(targetType));
        } else {
            histories = historyRepository.findAllByOrderByActionAtDesc();
        }
        
        if (limit != null && limit > 0 && histories.size() > limit) {
            histories = histories.subList(0, limit);
        }
        
        return histories.stream()
            .map(this::toDto)
            .toList();
    }

    @GetMapping("/articles/{articleId}/history")
    public List<HistoryDto> articleHistory(@PathVariable Long articleId) {
        currentActorService.requireGeneralAdmin();
        return historyRepository.findByArticleIdOrderByActionAtDesc(articleId).stream()
            .map(this::toDto)
            .toList();
    }

    @GetMapping("/variations/{variationId}/history")
    public List<HistoryDto> variationHistory(@PathVariable Long variationId) {
        currentActorService.requireGeneralAdmin();
        return historyRepository.findByVariationIdOrderByActionAtDesc(variationId).stream()
            .map(this::toDto)
            .toList();
    }

    private HistoryDto toDto(CatalogHistory h) {
        HistoryDto d = new HistoryDto();
        d.setId(h.getId());
        d.setTargetType(h.getTargetType() != null ? h.getTargetType().name() : null);
        d.setTargetId(h.getTargetId());
        d.setArticleId(h.getArticleId());
        d.setArticleName(h.getArticleName());
        d.setVariationId(h.getVariationId());
        d.setVariationLabel(h.getVariationLabel());
        d.setAction(h.getAction() != null ? h.getAction().name() : null);
        d.setActionLabel(toActionLabel(h.getAction()));
        d.setActionAt(h.getActionAt());
        d.setSummary(h.getSummary());
        d.setDetailsJson(h.getDetailsJson());
        d.setActorUserId(h.getActorUserId());
        d.setActorName(h.getActorFullName());
        d.setActorEmail(h.getActorEmail());
        d.setActorPhotoUrl(h.getActorUserId() != null ? "/api/admin/users/" + h.getActorUserId() + "/photo" : null);
        return d;
    }

    private String toActionLabel(CatalogAction action) {
        if (action == null) return null;
        return switch (action) {
            case CREATE -> "Created";
            case UPDATE -> "Edited";
            case DELETE -> "Deleted";
        };
    }

    @Data
    public static class HistoryDto {
        private Long id;
        private String targetType;
        private Long targetId;
        private Long articleId;
        private String articleName;
        private Long variationId;
        private String variationLabel;
        private String action;
        private String actionLabel;
        private Instant actionAt;
        private String summary;
        private String detailsJson;
        private Long actorUserId;
        private String actorName;
        private String actorEmail;
        private String actorPhotoUrl;
    }
}