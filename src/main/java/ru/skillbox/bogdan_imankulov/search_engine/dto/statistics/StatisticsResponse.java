package ru.skillbox.bogdan_imankulov.search_engine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
