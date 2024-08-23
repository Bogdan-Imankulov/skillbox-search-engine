CREATE SCHEMA IF NOT EXISTS search_engine;
USE search_engine;

CREATE TABLE site
(
    id          INT AUTO_INCREMENT                     NOT NULL,
    status      ENUM ('INDEXED', 'INDEXING', 'FAILED') NOT NULL,
    status_time datetime                               NOT NULL,
    last_error  TEXT                                   NULL,
    url         VARCHAR(255)                           NOT NULL,
    name        VARCHAR(255)                           NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE page
(
    id      INT AUTO_INCREMENT NOT NULL,
    site_id INT                NOT NULL,
    `path`  TEXT               NOT NULL,
    code    INT                NOT NULL,
    content MEDIUMTEXT         NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (site_id) REFERENCES site (id)
);

CREATE TABLE IF NOT EXISTS lemma
(
    id        INT AUTO_INCREMENT NOT NULL,
    site_id   INT                NOT NULL,
    lemma     VARCHAR(255)       NOT NULL,
    frequency INT                NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (site_id) REFERENCES site (id)
);

CREATE TABLE IF NOT EXISTS search_index
(
    id       INT AUTO_INCREMENT NOT NULL,
    page_id  INT                NOT NULL,
    lemma_id INT                NOT NULL,
    `rank`   FLOAT              NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (page_id) REFERENCES page (id),
    FOREIGN KEY (lemma_id) REFERENCES lemma (id)
);

