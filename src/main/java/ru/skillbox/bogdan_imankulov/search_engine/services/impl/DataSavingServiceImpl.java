package ru.skillbox.bogdan_imankulov.search_engine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.model.SearchIndex;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.LemmaRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.PageRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SearchIndexRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SiteRepository;
import ru.skillbox.bogdan_imankulov.search_engine.services.DataSavingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSavingServiceImpl implements DataSavingService {
	private final SiteRepository siteRepository;
	private final PageRepository pageRepository;

	private final LemmaRepository lemmaRepository;
	private final SearchIndexRepository indexRepository;


	@Override
	public synchronized void saveLemma(Lemma lemma) {
		lemmaRepository.save(lemma);
	}

	@Override
	public synchronized void savePage(Page page) {
		pageRepository.save(page);
	}

	@Override
	public synchronized void saveSiteModel(SiteModel siteModel) {
		siteRepository.save(siteModel);
	}

	@Override
	public synchronized void saveSearchIndex(SearchIndex searchIndex) {
		indexRepository.save(searchIndex);
	}


	@Override
	public synchronized void saveAllLemmas(Iterable<Lemma> lemmas) {
		lemmaRepository.saveAll(lemmas);
	}

	@Override
	public synchronized void saveAllPages(Iterable<Page> pages) {
		pageRepository.saveAll(pages);
	}

	@Override
	public synchronized void saveAllSiteModels(Iterable<SiteModel> siteModels) {
		siteRepository.saveAll(siteModels);
	}

	@Override
	public synchronized void saveAllSearchIndexes(Iterable<SearchIndex> searchIndexes) {
		indexRepository.saveAll(searchIndexes);
	}
}
