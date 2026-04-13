package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionUpdateDto {
    @NotBlank(message = "title must not be empty")
    @Size(max = 256, message = "title length must be less than 256")
    private String title;

    @NotBlank(message = "content must not be empty")
    private String content;

    @Size(max = 1024, message = "tags length must be less than 1024")
    private String tags;

    private String answer;

    private Integer difficulty;

    private Integer isMemberOnly;
}
