package ru.skillbox.bogdan_imankulov.search_engine.dto;

import lombok.Data;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.services.impl.SearchDataCreatingServiceImpl;

import java.util.List;

@Data
public class SearchData {
	private String site;
	private String siteName;
	private String uri;
	private String title;
	private String snippet;
	private float relevance;


	public SearchData(Page page, float relevance, List<Lemma> searchLemmas) {
		String content = page.getContent();
		this.title = SearchDataCreatingServiceImpl.formatTitle(content);
		this.snippet = SearchDataCreatingServiceImpl.formatSnippet(content, searchLemmas);
		this.relevance = relevance;
		this.site = page.getSite().getUrl();
		this.site = this.site.substring(0, this.site.length() - 1);
		this.siteName = page.getSite().getName();
		this.uri = page.getPath();
	}


}
