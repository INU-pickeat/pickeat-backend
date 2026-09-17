package com.pickeat.pickeatbackend.domain.survey.entity;

import com.pickeat.pickeatbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "surveys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 50)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyCategory category;

    private Integer estimatedMinutes;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean sharedToArchive;

    private LocalDateTime archivedAt;

    @Column(nullable = false)
    private boolean isDeleted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Survey(
            User creator,
            String title,
            String description,
            String target,
            SurveyCategory category,
            Integer estimatedMinutes,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.target = target;
        this.category = category;
        this.estimatedMinutes = estimatedMinutes;
        this.startDate = startDate;
        this.endDate = endDate;
        this.sharedToArchive = false;
        this.isDeleted = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
