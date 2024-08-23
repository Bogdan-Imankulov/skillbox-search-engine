package ru.skillbox.bogdan_imankulov.search_engine.services;

import ru.skillbox.bogdan_imankulov.search_engine.dto.IndexingResponse;
import ru.skillbox.bogdan_imankulov.search_engine.dto.SearchResponse;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;

import java.util.List;

public interface IndexingService {
	IndexingResponse startIndexing();

	IndexingResponse indexPage(String absolutePageUrl);

	SearchResponse search(String query,
	                      String site,
	                      int offset,
	                      int limit);

	List<Lemma> findLemmasByPercent(double percent);
}
