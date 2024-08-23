package ru.skillbox.bogdan_imankulov.search_engine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import ru.skillbox.bogdan_imankulov.search_engine.indexing.Connection;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;
import ru.skillbox.bogdan_imankulov.search_engine.model.enums.Status;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.LemmaRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.PageRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SearchIndexRepository;
import ru.skillbox.bogdan_imankulov.search_engine.repositories.SiteRepository;
import ru.skillbox.bogdan_imankulov.search_engine.services.impl.DataSavingServiceImpl;
import ru.skillbox.bogdan_imankulov.search_engine.util.MorphologyUtils;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@TestPropertySource(value = {"/application.yaml"})
@SpringBootTest
@AutoConfigureMockMvc
public class IndexingServiceTest {
	private final MorphologyUtils morphologyUtils = new MorphologyUtils();
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private LemmaRepository lemmaRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private SearchIndexRepository indexRepository;
	@Autowired
	private PageRepository pageRepository;
	@Autowired
	private DataSavingServiceImpl savingService;

	//works fine
	@Test
	@Sql(value = {"/before_test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	public void indexingTest() throws Exception {
		mockMvc.perform(get("/api/startIndexing"))
				.andDo(print());
	}


	//works fine
	@Test
	public void indexPageTest() throws Exception {
		mockMvc.perform(post("/api/indexPage").param("url", "skillbox.ru"))
				.andDo(print());
	}

	@Test
	public void statisticsTest() throws Exception {
		mockMvc.perform(get("/api/statistics"))
				.andDo(print());
	}

	@Test
	public void searchTest() throws Exception {
		mockMvc.perform(get("/api/search").param("query", "курс программирования со скидкой"))
				.andExpect(status().isOk())
				.andDo(print());
	}

	//Lemmas are adding correctly
	@Test
	@Sql(value = {"/before_test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	public void lemmaSavingTest() {
		Connection connection = new Connection(pageRepository, lemmaRepository, indexRepository, morphologyUtils);
		SiteModel dummy = new SiteModel();

		dummy.setUrl("http://dummy-url.com");
		dummy.setName("dummy");
		dummy.setLastError(null);
		dummy.setStatus(Status.INDEXING);
		dummy.setStatusTime(LocalDateTime.now());

		Page first = new Page();
		first.setContent("лемма лемма лемма лемма лемма лемма лемма лемма упоминается 8 раз, затем " +
				"лемма лемма лемма лемма лемма лемма лемма лемма лемма лемма лемма 12 раз");
		Page second = new Page();
		second.setContent("лемма лемма 2 раза, лемма один раз");

		first.setSite(dummy);
		first.setCode(HttpStatus.OK.value());
		first.setPath("/first");

		second.setSite(dummy);
		second.setCode(HttpStatus.OK.value());
		second.setPath("/second");


		savingService.saveSiteModel(dummy);
		savingService.savePage(first);
		savingService.savePage(second);

		connection.createLemmasAndIndexes(first);
		System.out.println("\n\n\n\n\n");
		System.out.println(lemmaRepository.findAllByLemma("лемма"));
		System.out.println("/=========================================================/");
		connection.createLemmasAndIndexes(second);
		System.out.println(lemmaRepository.findAllByLemma("лемма"));
		System.out.println("\n\n\n\n\n");
		Assert.isTrue(lemmaRepository.findAllByLemma("лемма").size() == 1, "Леммы складываются?");

	}
}
