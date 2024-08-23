package ru.skillbox.bogdan_imankulov.search_engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "search_index")
@NoArgsConstructor
@Getter
@Setter
public class SearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Page page;
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    private Lemma lemma;
    //Кол-во данной леммы на странице
    @Column(name = "`rank`", nullable = false)
    private Float rank;

    public SearchIndex(Page page, Lemma lemma, Float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + id + ", " +
                "rank = " + rank + ")";
    }
}
