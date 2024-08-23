package ru.skillbox.bogdan_imankulov.search_engine.indexing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import ru.skillbox.bogdan_imankulov.search_engine.dto.IndexingResponse;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.model.SearchIndex;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.LemmaRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.PageRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SearchIndexRepository;
import ru.skillbox.bogdan_imankulov.search_engine.services.impl.DataSavingServiceImpl;
import ru.skillbox.bogdan_imankulov.search_engine.services.impl.IndexingServiceImpl;
import ru.skillbox.bogdan_imankulov.search_engine.util.MorphologyUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class Connection extends RecursiveAction {

	private final PageRepository pageRepository;
	private final LemmaRepository lemmaRepository;
	private final SearchIndexRepository indexRepository;
	private Url url;
	private String firstUrl;
	private volatile Set<String> visitedLinks = Collections.synchronizedSet(new HashSet<>());
	private IndexingResponse indexingResponse;
	private List<Lemma> modelLemmas;
	private List<SearchIndex> searchIndexes;
	private SiteModel model;
	private MorphologyUtils morphUtils;
	private DataSavingServiceImpl savingService;

	public Connection(PageRepository pageRepository, LemmaRepository lemmaRepository, SearchIndexRepository indexRepository, MorphologyUtils morphUtils) {
		this.pageRepository = pageRepository;
		this.lemmaRepository = lemmaRepository;
		this.indexRepository = indexRepository;
		this.morphUtils = morphUtils;
		this.modelLemmas = new ArrayList<>();
		this.searchIndexes = new ArrayList<>();
	}

	public Connection(String firstUrl, SiteModel model,
	                  PageRepository pageRepository, LemmaRepository lemmaRepository,
	                  SearchIndexRepository indexRepository, MorphologyUtils morphUtils,
	                  DataSavingServiceImpl savingService) {
		this.url = new Url(firstUrl, 0);
		this.firstUrl = firstUrl;
		this.visitedLinks.add(firstUrl);
		this.model = model;
		this.indexingResponse = new IndexingResponse(true, "");
		this.modelLemmas = new ArrayList<>();
		this.searchIndexes = new ArrayList<>();

		this.pageRepository = pageRepository;
		this.lemmaRepository = lemmaRepository;
		this.indexRepository = indexRepository;
		this.morphUtils = morphUtils;
		this.savingService = savingService;
	}

	public static org.jsoup.Connection getDocument(String url) throws IOException {
		return Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT " + "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("https://www.google.com");
	}

	@Override
	public void compute() {
		getUrl(url.getUrl()).forEach(ForkJoinTask::join);
	}

	public HashSet<Connection> getUrl(String url) {
		HashSet<Connection> resultList = new HashSet<>();
		log.info("Parsing Url with address: " + url);
		try {
			Thread.sleep(100);
			org.jsoup.Connection connection = getDocument(url);
			int statusCode = connection.execute().statusCode();
			if (statusCode < 400) {
				Document doc = connection.get();
				Elements elements = doc.select("a");
				Page page = createPageFromDocument(url, doc);
				model.setStatusTime(LocalDateTime.now());
				page = savePageOrGetExisting(page);
				saveLemmasAndIndexes(page);
				forkNewConnection(elements, resultList);
			}
		} catch (IOException | InterruptedException e) {
			String msg = "Could not index the pages, error is: " + e.getMessage();
			log.error(msg);
			indexingResponse.setError(msg);
			indexingResponse.setSuccess(false);

		}
		return resultList;
	}

	private void saveLemmasAndIndexes(Page page) throws InterruptedException {
		createLemmasAndIndexes(page);
		savingService.saveAllLemmas(modelLemmas);
		modelLemmas = new ArrayList<>();
		Thread.sleep(100);
		savingService.saveAllSearchIndexes(searchIndexes);
		searchIndexes = new ArrayList<>();
	}

	private Page savePageOrGetExisting(Page page) {
		synchronized (pageRepository) {
			Page foundPage = pageRepository.findByPath(page.getPath());
			if (foundPage != null && foundPage.getSite().getUrl().equals(model.getUrl())) {
				page = foundPage;
			} else {
				savingService.savePage(page);
			}
		}
		return page;
	}

	private Page createPageFromDocument(String url, Document doc) {
		String relativeUrl = "/";
		if (!url.equals(model.getUrl())) {
			//get all after https://*domain_name*/

			relativeUrl = url.substring(url.indexOf('/', 8));
		}
		if (IndexingServiceImpl.containsLanguageCode(relativeUrl)) {
			relativeUrl = relativeUrl.substring(3);
		}
		relativeUrl = relativeUrl.replaceAll("/+", "/");
		String documentHtmlBody = doc.outerHtml();
		return new Page(model, relativeUrl, HttpStatus.OK.value(), documentHtmlBody);
	}

	private void forkNewConnection(Elements elements, HashSet<Connection> resultList) {
		for (Element el : elements) {
			String link = el.attr("abs:href");
			if (!link.isEmpty() && !link.contains("#") && !visitedLinks.contains(link) && link.startsWith(firstUrl)) {
				if (!link.endsWith("/")) {
					link += "/";
				}
				Connection childConnection = new Connection(link,
						this.model, this.pageRepository,
						this.lemmaRepository, this.indexRepository, this.morphUtils, this.savingService);
				childConnection.fork();
				resultList.add(childConnection);
			}
		}
	}

	public void createLemmasAndIndexes(Page page) {
		Map<String, Integer> lemmaAndFreq = morphUtils.getLemmasAndTheirFrequency(page.getContent());
		for (Map.Entry<String, Integer> entry : lemmaAndFreq.entrySet()) {
			String word = entry.getKey();
			Lemma lemma = new Lemma(word, entry.getValue(), page.getSite());
			SearchIndex index;
			List<Lemma> lemmasInDb = lemmaRepository.findAllByLemma(word);
			if (lemmasInDb.isEmpty()) {
				savingService.saveLemma(lemma);
				index = new SearchIndex(page, lemma, (float) lemma.getFrequency());
				savingService.saveSearchIndex(index);
			} else if (lemmasInDb.size() == 1) {
				Lemma found = lemmaRepository.findByLemma(word);
				found.setFrequency(found.getFrequency() + lemma.getFrequency());
				index = new SearchIndex(page, found, (float) found.getFrequency());
				modelLemmas.add(found);
				searchIndexes.add(index);
			}
		}
	}

}
