package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatSessionCreateDto {
    @Size(max = 128, message = "title length must be less than 128")
    private String title;
}
