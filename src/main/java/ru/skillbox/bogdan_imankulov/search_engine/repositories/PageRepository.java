package ru.skillbox.bogdan_imankulov.search_engine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.skillbox.bogdan_imankulov.search_engine.model.Page;
import ru.skillbox.bogdan_imankulov.search_engine.model.SiteModel;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
	@Query(
			value = "SELECT * FROM search_engine.page pageObj WHERE pageObj.path = :path",
			nativeQuery = true
	)
	Page findByPath(@Param("path") String path);

	@Query(
			value = "SELECT * FROM search_engine.page pageObj WHERE pageObj.path = :path AND pageObj.site_id = :site",
			nativeQuery = true
	)
	Page findByPathAndSite(@Param("path") String path, @Param("site") SiteModel site);
}
