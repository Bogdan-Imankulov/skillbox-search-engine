package ru.skillbox.bogdan_imankulov.search_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndexingResponse {
    private boolean success;
    //msg of an error, blank otherwise
    private String error;

}
