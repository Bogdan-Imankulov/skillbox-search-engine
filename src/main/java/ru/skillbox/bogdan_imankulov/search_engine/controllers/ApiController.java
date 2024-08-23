package ru.skillbox.bogdan_imankulov.search_engine.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.skillbox.bogdan_imankulov.search_engine.dto.IndexingResponse;
import ru.skillbox.bogdan_imankulov.search_engine.dto.SearchResponse;
import ru.skillbox.bogdan_imankulov.search_engine.dto.statistics.StatisticsResponse;
import ru.skillbox.bogdan_imankulov.search_engine.services.IndexingService;
import ru.skillbox.bogdan_imankulov.search_engine.services.StatisticsService;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {

	private final StatisticsService statisticsService;
	private final IndexingService indexingService;

	public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
		this.statisticsService = statisticsService;
		this.indexingService = indexingService;
	}

	@GetMapping("/statistics")
	public ResponseEntity<StatisticsResponse> statistics() {
		return ResponseEntity.ok(statisticsService.getStatistics());
	}

	@PostMapping("/indexPage")
	public ResponseEntity<IndexingResponse> indexPage(@RequestBody String url) {
		return ResponseEntity.of(
				Optional.of(indexingService.indexPage(url))
		);
	}

	@GetMapping(path = "/startIndexing",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IndexingResponse> startIndexing() {
		IndexingResponse indexingResponse = indexingService.startIndexing();

		return ResponseEntity.of(Optional.of(indexingResponse));
	}

	@GetMapping(path = "/search")
	public @ResponseBody ResponseEntity<SearchResponse> search(@RequestParam String query,
	                                                           @RequestParam(required = false, defaultValue = "") String site,
	                                                           @RequestParam(required = false, defaultValue = "0") int offset,
	                                                           @RequestParam(required = false, defaultValue = "20") int limit) {


		return ResponseEntity.of(Optional.of(indexingService.search(query, site, offset, limit)));
	}

	@GetMapping(path = "/findByPercent/{percent}")
	public int findLemmasByPercent(@PathVariable double percent) {

		return indexingService.findLemmasByPercent(percent).size();
	}

}
