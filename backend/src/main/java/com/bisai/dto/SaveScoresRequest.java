package com.bisai.dto;

import com.bisai.entity.ScoreResult;
import lombok.Data;

import java.util.List;

@Data
public class SaveScoresRequest {
    private List<ScoreResult> scores;
    private String comment;
    private String expectedUpdatedAt;
}
