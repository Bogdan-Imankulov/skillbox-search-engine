package ru.skillbox.bogdan_imankulov.search_engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@NoArgsConstructor
@Setter
@Entity
@Table(name = "lemma")
@ToString(exclude = "site")
public class Lemma {
	@Getter
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Integer id;
	//Начальная форма(Именительный падеж, единственное число)
	@Column(name = "lemma", nullable = false)
	private String lemma;
	//Количество страниц на которых есть лемма, не больше чем общее число слов на сайте
	@Column(name = "frequency", nullable = false)
	@Getter
	private Integer frequency;
	@ManyToOne(fetch = FetchType.EAGER)
	@Getter
	private SiteModel site;

	public Lemma(String lemma, Integer frequency, SiteModel site) {
		this.lemma = lemma;
		this.frequency = frequency;
		this.site = site;
	}


	public String getLemmaWord() {
		return lemma;
	}
}
