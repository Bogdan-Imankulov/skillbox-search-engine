package ru.skillbox.bogdan_imankulov.search_engine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.model.SearchIndex;

import java.util.List;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {

	List<SearchIndex> findAllByLemma(Lemma lemma);

	List<SearchIndex> findAllByPage(Page page);
}
