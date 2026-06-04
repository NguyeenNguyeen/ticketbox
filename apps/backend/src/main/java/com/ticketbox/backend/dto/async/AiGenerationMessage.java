package com.ticketbox.backend.dto.async;

import java.io.Serializable;

/**
 * Message payload for generating an artist bio via AI.
 */
public class AiGenerationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long artistId;
    private String promptParams;

    public AiGenerationMessage() {
    }

    public AiGenerationMessage(Long artistId, String promptParams) {
        this.artistId = artistId;
        this.promptParams = promptParams;
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public String getPromptParams() {
        return promptParams;
    }

    public void setPromptParams(String promptParams) {
        this.promptParams = promptParams;
    }

    @Override
    public String toString() {
        return "AiGenerationMessage{" +
                "artistId=" + artistId +
                ", promptParams='" + promptParams + '\'' +
                '}';
    }
}
