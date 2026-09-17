package com.pickeat.pickeatbackend.domain.survey.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false)
    private boolean isRequired;

    @Column(nullable = false)
    private boolean allowMultiple;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Question(
            Survey survey,
            int questionOrder,
            QuestionType type,
            String content,
            boolean isRequired,
            boolean allowMultiple
    ) {
        this.survey = survey;
        this.questionOrder = questionOrder;
        this.type = type;
        this.content = content;
        this.isRequired = isRequired;
        this.allowMultiple = allowMultiple;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
