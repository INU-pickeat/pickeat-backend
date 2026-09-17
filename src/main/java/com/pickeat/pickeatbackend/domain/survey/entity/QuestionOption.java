package com.pickeat.pickeatbackend.domain.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "question_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_order", nullable = false)
    private int optionOrder;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public QuestionOption(Question question, int optionOrder, String content) {
        this.question = question;
        this.optionOrder = optionOrder;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
