package ru.skillbox.bogdan_imankulov.search_engine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {

	@Query(
			value = "SELECT * FROM search_engine.site siteModel WHERE siteModel.url = :url",
			nativeQuery = true
	)
	SiteModel findByUrl(@Param("url") String url);
}
