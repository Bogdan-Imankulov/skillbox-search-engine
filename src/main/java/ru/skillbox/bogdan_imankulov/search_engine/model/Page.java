package ru.skillbox.bogdan_imankulov.search_engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "page", indexes = @Index(name = "pathIndex", columnList = "path", unique = true))
public class Page {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Integer id;
	@ManyToOne(fetch = FetchType.EAGER)
	private SiteModel site;
	//Адрес от корня, .../path/ ;
	@Column(name = "path", nullable = false, unique = true, columnDefinition = "TEXT")
	private String path;
	//HttpCode
	@Column(name = "code", nullable = false)
	private Integer code;
	//html код страницы
	@Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
	private String content;
	@OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
	private List<SearchIndex> indexes;


	public Page(SiteModel site, String path, Integer code, String content) {
		this.site = site;
		this.path = path;
		this.code = code;
		this.content = content;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" +
				"id = " + id + ", " +
				"path = " + path + ", " +
				"code = " + code + ", " +
				"content = " + content + ")";
	}
}
