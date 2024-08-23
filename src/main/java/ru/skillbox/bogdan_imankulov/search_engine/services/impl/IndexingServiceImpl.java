package ru.skillbox.bogdan_imankulov.search_engine.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.skillbox.bogdan_imankulov.search_engine.config.Site;
import ru.skillbox.bogdan_imankulov.search_engine.config.SitesList;
import ru.skillbox.bogdan_imankulov.search_engine.dto.IndexingResponse;
import ru.skillbox.bogdan_imankulov.search_engine.dto.SearchData;
import ru.skillbox.bogdan_imankulov.search_engine.dto.SearchResponse;
import ru.skillbox.bogdan_imankulov.search_engine.indexing.Connection;
import ru.skillbox.bogdan_imankulov.search_engine.indexing.SiteIndexingService;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.model.SearchIndex;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.LemmaRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.PageRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SearchIndexRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SiteRepository;
import ru.skillbox.bogdan_imankulov.search_engine.services.IndexingService;
import ru.skillbox.bogdan_imankulov.search_engine.util.MorphologyUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

	private final SitesList sitesList;
	private final SiteRepository siteRepository;
	private final PageRepository pageRepository;

	private final LemmaRepository lemmaRepository;
	private final SearchIndexRepository indexRepository;

	private final DataSavingServiceImpl savingService;
	private final MorphologyUtils morphUtils = new MorphologyUtils();

	private static Map<Page, Float> findPagesAbsoluteRelevance(List<Page> queryPages, double[] eachPageRelevance) {
		Arrays.sort(eachPageRelevance);
		double maxRelevance = eachPageRelevance[eachPageRelevance.length - 1];
		//divide each page's relevance per max relevance to find relative reference
		double[] relativeRelevance = Arrays.stream(eachPageRelevance).map(i -> i / maxRelevance).toArray();

		//compare pages and their relevance
		Map<Page, Float> pageRelevanceMap = new HashMap<>();
		for (int i = 0; i < queryPages.size(); i++) {
			pageRelevanceMap.put(queryPages.get(i), (float) relativeRelevance[i]);
		}
		return pageRelevanceMap;
	}

	private static List<IndexingResponse> futuresToResponses(List<Future<IndexingResponse>> futureResponses) {
		List<IndexingResponse> allResponses;
		allResponses = futureResponses.stream().map(future -> {
			IndexingResponse response = new IndexingResponse(false, "null");
			try {
				response = future.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Error while converting future to response: " + e.getMessage());
				log.error(Arrays.toString(e.getStackTrace()));
			}
			return response;
		}).toList();
		return allResponses;
	}

	public static boolean containsLanguageCode(String absolutePageUrl) {
		String pattern = "/(ru|en|de|es|fr|it|pt|nl|pl|sv|no|da|fi|cs|sk|hu|bg|ro|hr|sr|mk|sl|lt|lv|et|tr|el|ja|ko|zh)/[-A-z0-9./?=&#%]*";
		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(absolutePageUrl);
		return matcher.find();
	}

	@Override
	public IndexingResponse startIndexing() {
		List<Site> sites = sitesList.getSites();
		List<IndexingResponse> allResponses;
		log.info("Starting indexing");

		cleanRepositories();
		//execute every single site with a new forkJoinPool
		ExecutorService executors = Executors.newFixedThreadPool(sites.size());

		List<Future<IndexingResponse>> futureResponses = getFutures(sites, executors);

		executors.shutdown();
		allResponses = futuresToResponses(futureResponses);

		//check if all sites were indexed successfully
		for (IndexingResponse response : allResponses) {
			if (!response.isSuccess()) {
				log.error("Indexing is not completed");
				return new IndexingResponse(false, "Не удалось индексировать сайты");
			}
		}
		log.info("Indexing Succeed");
		return new IndexingResponse(true, "Индексация успешна");
	}

	private List<Future<IndexingResponse>> getFutures(List<Site> sites, ExecutorService executors) {
		List<Future<IndexingResponse>> futureResponses = new ArrayList<>();
		for (Site site : sites) {
			Future<IndexingResponse> futureResponse =
					executors.submit(new SiteIndexingService(siteRepository, pageRepository,
							lemmaRepository, indexRepository, site, savingService));
			futureResponses.add(futureResponse);
		}
		return futureResponses;
	}

	private void cleanRepositories() {
		indexRepository.deleteAll();
		lemmaRepository.deleteAll();
		pageRepository.deleteAll();
		siteRepository.deleteAll();
		log.info("Repositories cleared");
	}

	@Override
	public IndexingResponse indexPage(String absolutePageUrl) {
		//Check if wrong pageUrl
		absolutePageUrl = formatUrl(absolutePageUrl);
		log.info("Starting indexing page with url: " + absolutePageUrl);
		//delete https://*site_name/
		String relativePageUrl = absolutePageUrl.substring(absolutePageUrl.indexOf('/', 8));
		boolean hasLanguageCode = containsLanguageCode(relativePageUrl);
		if (hasLanguageCode) {
			relativePageUrl = relativePageUrl.substring(3);
		}
		String urlWithProtocol = absolutePageUrl;
		//base url = https://*site_name/
		if (absolutePageUrl.contains("https://")) {
			absolutePageUrl = absolutePageUrl.substring(8);
		} else if (absolutePageUrl.contains("http://")) {
			absolutePageUrl = absolutePageUrl.substring(7);
		} else {
			urlWithProtocol = "http://" + urlWithProtocol;
		}
		String URL = absolutePageUrl.replace(relativePageUrl, "/");
		SiteModel parentSite = siteRepository.findByUrl("https://" + URL);
		if (hasLanguageCode) {
			String[] urlSplit = absolutePageUrl.replace(relativePageUrl, "/").split("/");
			URL = urlSplit[0] + "/" + urlSplit[1] + "/";
			parentSite = siteRepository.findByUrl("https://" + URL);
		}

		if (parentSite != null) {
			String parentUrl = parentSite.getUrl();
			if (parentUrl.contains("https://")) {
				parentUrl = parentUrl.substring(8);
			} else if (absolutePageUrl.contains("http://")) {
				parentUrl = parentUrl.substring(7);
			}
			if (!absolutePageUrl.startsWith(parentUrl)) {
				return new IndexingResponse(false, "Страница с адресом " + absolutePageUrl + " не принадлежит ни к одному индексируемому сайту");
			}
			//delete old page if exists
			if (pageRepository.findByPathAndSite(relativePageUrl, parentSite) != null) {
				pageRepository.delete(Objects.requireNonNull(pageRepository.findByPathAndSite(relativePageUrl, parentSite)));
				log.info("Deleted old page");
			}
			//Create entity
			String pageContent;
			try {
				pageContent = Connection.getDocument(urlWithProtocol).get().outerHtml();
			} catch (IOException e) {
				log.warn("Couldn't index page with url: " + absolutePageUrl);
				log.warn("Couldn't get page's content");
				return new IndexingResponse(false, "Не удалось получить содержимое страницы с адресом: " + absolutePageUrl);
			}
			saveIndexedPage(relativePageUrl, parentSite, pageContent);
			log.info("Page indexed successfully");
			return new IndexingResponse(true, "Индексация успешна");
		}
		log.warn("Couldn't index page with url: " + absolutePageUrl);
		return new IndexingResponse(false, "Страница с адресом " + absolutePageUrl + " не принадлежит ни к одному индексируемому сайту");
	}

	private void saveIndexedPage(String relativePageUrl, SiteModel parentSite, String pageContent) {
		log.info("Saving indexing page...");
		Page page = new Page(parentSite, relativePageUrl, HttpStatus.OK.value(), pageContent);
		List<Lemma> lemmas = new ArrayList<>();
		List<SearchIndex> indexes = new ArrayList<>();
		setLemmasAndIndexes(pageContent, parentSite, page, lemmas, indexes);
		savingService.savePage(page);

		List<Lemma> duplicatedLemmas = saveLemmas(lemmas, parentSite);

		updateIndexesAndTheirLemmas(indexes, duplicatedLemmas);

		savingService.saveAllSearchIndexes(indexes);
		savingService.saveSiteModel(parentSite);
		log.info("Page saved successfully");
	}

	private void updateIndexesAndTheirLemmas(List<SearchIndex> indexes, List<Lemma> duplicatedLemmas) {
		for (SearchIndex index : indexes) {
			for (Lemma duplicatedLemma : duplicatedLemmas) {
				if (index.getLemma().equals(duplicatedLemma)) {
					List<Lemma> list = lemmaRepository.findAllBySite(duplicatedLemma.getSite());
					Lemma updatedLemmaForSearchIndex = list.stream().filter(lemma -> lemma.getLemmaWord().equals(duplicatedLemma.getLemmaWord()))
							.toList().get(0);
					index.setLemma(updatedLemmaForSearchIndex);
				}
			}
		}
	}

	private List<Lemma> saveLemmas(List<Lemma> lemmas, SiteModel parentSite) {
		log.info("Started saving lemmas...");
		List<Lemma> duplicatedLemmas = new ArrayList<>();
		for (Lemma lemma : lemmas) {
			//check for duplicate lemmas
			List<Lemma> parentSiteLemmas = lemmaRepository.findAllBySite(parentSite);
			List<Lemma> existingLemmas = parentSiteLemmas.stream()
					.filter(current -> current.getLemmaWord().equals(lemma.getLemmaWord()))
					.toList();
			if (existingLemmas.isEmpty()) {
				savingService.saveLemma(lemma);
			} else {
				//increase lemma frequency if current lemma already exists
				Lemma existingLemma = existingLemmas.get(0);
				existingLemma.setFrequency(existingLemma.getFrequency() + 1);
				savingService.saveLemma(existingLemma);
				duplicatedLemmas.add(lemma);
			}
		}
		return duplicatedLemmas;
	}

	@Override
	public SearchResponse search(String query, String site, int offset, int limit) {
		//query as lemma list
		log.info("Searching...");
		Map<String, Integer> queryFrequency = morphUtils.getLemmasAndTheirFrequency(query);
		List<Lemma> queryLemmas = getQueryLemmas(site, queryFrequency);

		//drop all common lemmas and sort by frequency asc
		queryLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));

		//find all pages that contain every lemma in query
		List<Page> queryPages = getPagesThatContainAnyOfQueryLemmas(queryLemmas);
		List<Page> lemmasToDelete = new ArrayList<>();
		getPageThatContainsEveryQueryLemma(queryLemmas, queryPages, lemmasToDelete);

		queryPages = filterPagesBySite(site, queryPages);

		//if no queryPages return error;
		if (queryPages.isEmpty()) {
			log.info("No pages found");
			return new SearchResponse(false, 0, null, "Нет сайтов соответствующих поиску");
		} else if (queryPages.size() == 1) {
			log.info("Pages found: 1");
			SearchData searchData = new SearchData(queryPages.get(0), (float) 1.0, queryLemmas);
			return new SearchResponse(true, 1, List.of(searchData));
		}
		double[] eachPageRelevance = findPagesRelevance(query, queryPages);
		Map<Page, Float> pageRelevanceMap = findPagesAbsoluteRelevance(queryPages, eachPageRelevance);

		//sort pages by relevance
		List<Map.Entry<Page, Float>> pagesSortedByRelevance = new ArrayList<>(pageRelevanceMap.entrySet());
		pagesSortedByRelevance.sort((entry1, entry2) -> Float.compare(entry2.getValue(), entry1.getValue()));

		//map pagesSortedByRelevance to SearchData list
		List<SearchData> searchResult = pagesSortedByRelevance.stream().map(entry -> new SearchData(entry.getKey(), entry.getValue(), queryLemmas)).toList();
		log.info("Pages found: " + searchResult.size());
		return new SearchResponse(true, searchResult.size(), searchResult);
	}

	private List<Page> filterPagesBySite(String site, List<Page> queryPages) {
		if (!site.isEmpty()) {
			queryPages = queryPages.stream()
					.filter(page -> page.getSite().equals(siteRepository.findByUrl(site)))
					.collect(Collectors.toList());
		}
		return queryPages;
	}

	private double[] findPagesRelevance(String query, List<Page> queryPages) {
		double[] eachPageRelevance = new double[queryPages.size()];
		for (int i = 0; i < queryPages.size(); i++) {
			Page page = queryPages.get(i);
			List<SearchIndex> indexes = indexRepository.findAllByPage(page).stream().parallel().filter(index -> query.contains(index.getLemma().getLemmaWord())).toList();
			for (SearchIndex index : indexes) {
				eachPageRelevance[i] = index.getRank();
			}
		}
		return eachPageRelevance;
	}

	private void getPageThatContainsEveryQueryLemma(List<Lemma> queryLemmas, List<Page> queryPages, List<Page> lemmasToDelete) {
		for (Page page : queryPages) {
			List<SearchIndex> indexes = indexRepository.findAllByPage(page);
			Set<String> lemmaList = indexes.stream().parallel().map(SearchIndex::getLemma).map(Lemma::getLemmaWord).collect(Collectors.toSet());
			Set<String> queryWords = queryLemmas.stream().map(Lemma::getLemmaWord).collect(Collectors.toSet());
			if (!lemmaList.containsAll(queryWords)) {
				lemmasToDelete.add(page);
			}
		}
		queryPages.removeAll(lemmasToDelete);
	}

	private List<Page> getPagesThatContainAnyOfQueryLemmas(List<Lemma> queryLemmas) {
		List<Page> queryPages = new ArrayList<>();
		for (Lemma queryLemma : queryLemmas) {
			List<SearchIndex> lemmaIndexes = indexRepository.findAllByLemma(
					lemmaRepository.findByLemma(queryLemma.getLemmaWord()));
			lemmaIndexes.forEach(index -> {
				if (!queryPages.contains(index.getPage())) {
					queryPages.add(index.getPage());
				}
			});
		}
		return queryPages;
	}

	private List<Lemma> getQueryLemmas(String site, Map<String, Integer> queryFrequency) {
		List<Lemma> queryLemmas = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : queryFrequency.entrySet()) {
			String lemma = entry.getKey();
			Integer frequency = entry.getValue();
			if (site.isEmpty()) {
				Lemma lem = lemmaRepository.findAllByLemma(lemma).get(0);
				SiteModel model = lem.getSite();
				queryLemmas.add(new Lemma(lemma, frequency, model));

			} else {
				SiteModel siteModel = siteRepository.findByUrl(site);
				queryLemmas.add(new Lemma(lemma, frequency, siteModel));
			}
		}
		return queryLemmas;
	}

	private void setLemmasAndIndexes(String pageContent, SiteModel parentSite, Page page, List<Lemma> lemmas, List<SearchIndex> indexes) {
		Map<String, Integer> frequencies = morphUtils.getLemmasAndTheirFrequency(pageContent);
		for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
			Lemma lemma = new Lemma(entry.getKey(), entry.getValue(), parentSite);
			SearchIndex index = new SearchIndex(page, lemma, entry.getValue().floatValue());
			lemmas.add(lemma);
			indexes.add(index);
		}
	}

	private String formatUrl(String urlToFormat) {
		urlToFormat = urlToFormat.replaceAll("%3[Aa]", ":");
		urlToFormat = urlToFormat.replaceAll("%2[Ff]", "/");
		urlToFormat = urlToFormat.replace("url=", "");
		if (!urlToFormat.endsWith("/")) {
			urlToFormat += "/";
		}
		return urlToFormat;
	}

	public List<Lemma> findLemmasByPercent(double percent) {
		if (percent >= 100 || percent <= 0) {
			throw new IllegalArgumentException("Percent must be between 0 and 100 exclusive");
		}

		long totalLemmasCount = lemmaRepository.count();

		if (totalLemmasCount == 0) {
			throw new RuntimeException("no lemmas indexed");
		}

		System.out.println(totalLemmasCount);
		System.out.println(totalLemmasCount * (1 - (percent / 100)));

		List<Lemma> lemmas = lemmaRepository.findAll();
		lemmas.sort(Comparator.comparingInt(Lemma::getFrequency));
		lemmas = lemmas.subList(0, (int) (totalLemmasCount * percent / 100));
		return lemmas;
	}
}
