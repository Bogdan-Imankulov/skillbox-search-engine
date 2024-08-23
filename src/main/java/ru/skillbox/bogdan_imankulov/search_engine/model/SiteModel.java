package ru.skillbox.bogdan_imankulov.search_engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.skillbox.bogdan_imankulov.search_engine.model.enums.Status;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
@NoArgsConstructor
@Getter
@Setter
public class SiteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('INDEXED', 'INDEXING', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;
    //Время статуса, обновляется при каждой новой странице на сайте
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    //Текст ошибки индексации || null
    @Column(name = "last_error", nullable = true, columnDefinition = "TEXT")
    private String lastError;
    //Главная страница, что-то вроде домена
    @Column(name = "url", nullable = false)
    private String url;
    //имя сайта
    @Column(name = "name", nullable = false)
    private String name;
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Page> pages = new ArrayList<>();
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Lemma> lemmas = new ArrayList<>();

    public SiteModel(Status status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + id + ", " +
                "status = " + status + ", " +
                "statusTime = " + statusTime + ", " +
                "lastError = " + lastError + ", " +
                "url = " + url + ", " +
                "name = " + name + ")";
    }
}
