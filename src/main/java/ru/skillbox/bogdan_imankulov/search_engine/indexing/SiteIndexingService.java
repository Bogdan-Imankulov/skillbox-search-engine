package ru.skillbox.bogdan_imankulov.search_engine.indexing;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.skillbox.bogdan_imankulov.search_engine.config.Site;
import ru.skillbox.bogdan_imankulov.search_engine.dto.IndexingResponse;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;
import ru.skillbox.bogdan_imankulov.search_engine.model.enums.Status;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.LemmaRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.PageRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SearchIndexRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SiteRepository;
import ru.skillbox.bogdan_imankulov.search_engine.services.impl.DataSavingServiceImpl;
import ru.skillbox.bogdan_imankulov.search_engine.util.MorphologyUtils;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Data
public class SiteIndexingService implements Callable<IndexingResponse> {
	private final SiteRepository siteRepository;
	private final PageRepository pageRepository;

	private final LemmaRepository lemmaRepository;
	private final SearchIndexRepository indexRepository;

	private final MorphologyUtils morphUtils = new MorphologyUtils();
	private final DataSavingServiceImpl savingService;
	private ForkJoinPool forkJoinPool;
	private Site site;

	public SiteIndexingService(SiteRepository siteRepository, PageRepository pageRepository,
	                           LemmaRepository lemmaRepository, SearchIndexRepository indexRepository,
	                           Site site, DataSavingServiceImpl savingService) {
		this.siteRepository = siteRepository;
		this.pageRepository = pageRepository;
		this.lemmaRepository = lemmaRepository;
		this.indexRepository = indexRepository;
		this.site = site;
		this.savingService = savingService;
		forkJoinPool = new ForkJoinPool();
	}

	@Override
	public IndexingResponse call() {
		String url = site.getUrl();
		SiteModel newModel = new SiteModel(Status.INDEXING, LocalDateTime.now(), "", site.getUrl(), site.getName());
		savingService.saveSiteModel(newModel);
		log.info("Executing forkJoin");
		Connection connection = new Connection(url, newModel, pageRepository, lemmaRepository, indexRepository, morphUtils, savingService);
		forkJoinPool.invoke(connection);
		newModel.setStatus(Status.INDEXED);
		//get current site's response
		IndexingResponse response = connection.getIndexingResponse();
		log.info("Site: " + newModel.getName() + " success: " + response.isSuccess());
		newModel.setStatus(response.isSuccess() ? Status.INDEXED : Status.FAILED);
		siteRepository.save(newModel);
		return response;
	}
}
