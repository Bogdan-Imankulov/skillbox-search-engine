package ru.skillbox.bogdan_imankulov.search_engine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.skillbox.bogdan_imankulov.search_engine.config.Site;
import ru.skillbox.bogdan_imankulov.search_engine.config.SitesList;
import ru.skillbox.bogdan_imankulov.search_engine.dto.statistics.DetailedStatisticsItem;
import ru.skillbox.bogdan_imankulov.search_engine.dto.statistics.StatisticsData;
import ru.skillbox.bogdan_imankulov.search_engine.dto.statistics.StatisticsResponse;
import ru.skillbox.bogdan_imankulov.search_engine.dto.statistics.TotalStatistics;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.LemmaRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.PageRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SiteRepository;
import ru.skillbox.bogdan_imankulov.search_engine.services.StatisticsService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
	private final SitesList sites;
	private final PageRepository pageRepository;
	private final LemmaRepository lemmaRepository;
	private final SiteRepository siteRepository;

	@Override
	public StatisticsResponse getStatistics() {
		TotalStatistics total = new TotalStatistics();
		total.setSites(sites.getSites().size());
		total.setPages((int) pageRepository.count());
		total.setLemmas((int) lemmaRepository.count());
		total.setIndexing(true);

		List<DetailedStatisticsItem> detailed = new ArrayList<>();
		List<Site> sitesList = sites.getSites();
		for (Site site : sitesList) {
			DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem();
			detailedStatisticsItem.setName(site.getName());
			detailedStatisticsItem.setUrl(site.getUrl());
			SiteModel currentModel = siteRepository.findByUrl(site.getUrl());
			if (currentModel != null) {

				int pagesCount = countPages(currentModel);
				detailedStatisticsItem.setPages(pagesCount);


				int lemmasCount = countLemmas(currentModel);
				detailedStatisticsItem.setLemmas(lemmasCount);

				detailedStatisticsItem.setStatus(currentModel.getStatus().toString());
				detailedStatisticsItem.setError(currentModel.getLastError());

				LocalDateTime dateTime = currentModel.getStatusTime();
				//Convert dateTime to java.util.Date, then get long value
				Date date = Date.from(dateTime
						.atZone(ZoneId.systemDefault())
						.toInstant());
				detailedStatisticsItem.setStatusTime(date.getTime());
				total.setPages(total.getPages() + pagesCount);
				total.setLemmas(total.getLemmas() + lemmasCount);
				detailed.add(detailedStatisticsItem);
			}
		}

		StatisticsResponse response = new StatisticsResponse();
		StatisticsData data = new StatisticsData();
		data.setTotal(total);
		data.setDetailed(detailed);
		response.setStatistics(data);
		response.setResult(true);
		return response;
	}

	private int countLemmas(SiteModel currentModel) {
		return (int) lemmaRepository.findAll().stream()
				.filter(lemma -> lemma.getSite().equals(currentModel))
				.count();
	}

	private int countPages(SiteModel currentModel) {
		return (int) pageRepository.findAll().stream()
				.filter(page -> page.getSite().equals(currentModel))
				.count();
	}
}
