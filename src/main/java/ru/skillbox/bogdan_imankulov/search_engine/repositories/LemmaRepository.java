package ru.skillbox.bogdan_imankulov.search_engine.repositories;

import org.hibernate.annotations.SQLInsert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;

import java.util.List;

@Repository
@SQLInsert(sql = "INSERT INTO lemma VALUES () ON DUPLICATE KEY UPDATE lemma.frequency = lemma.frequency + 1")
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

	@Query(
			value = "SELECT * FROM Lemma lemmaObj WHERE lemmaObj.lemma = :lemmaParam",
			nativeQuery = true
	)
	Lemma findByLemma(@Param("lemmaParam") String lemma);

	@Query(
			value = "SELECT * FROM Lemma lemmaObj WHERE lemmaObj.lemma = :lemmaParam",
			nativeQuery = true
	)
	List<Lemma> findAllByLemma(@Param("lemmaParam") String lemma);

	@Query(
			value = "SELECT * FROM Lemma lemmaObj WHERE lemmaObj.site_id = :siteParam",
			nativeQuery = true
	)
	List<Lemma> findAllBySite(@Param("siteParam") SiteModel site);
}
