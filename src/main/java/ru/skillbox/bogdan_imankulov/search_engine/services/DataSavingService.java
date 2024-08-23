package ru.skillbox.bogdan_imankulov.search_engine.services;

import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.model.SearchIndex;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;

public interface DataSavingService {
	void saveLemma(Lemma lemma);

	void savePage(Page page);

	void saveSiteModel(SiteModel siteModel);

	void saveSearchIndex(SearchIndex searchIndex);

	void saveAllLemmas(Iterable<Lemma> lemmas);

	void saveAllPages(Iterable<Page> pages);

	void saveAllSiteModels(Iterable<SiteModel> siteModels);

	void saveAllSearchIndexes(Iterable<SearchIndex> searchIndexes);
}
