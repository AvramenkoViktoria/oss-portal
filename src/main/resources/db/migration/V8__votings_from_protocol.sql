-- Votings are read from the uploaded protocol and carry only the totals the protocol states.
DROP TABLE vote_record;

ALTER TABLE voting
    ADD COLUMN votes_absent INT NOT NULL DEFAULT 0,
    ADD COLUMN present      INT NOT NULL DEFAULT 0;
