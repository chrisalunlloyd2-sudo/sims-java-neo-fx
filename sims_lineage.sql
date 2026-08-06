
CREATE TABLE IF NOT EXISTS organism_lineage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    agent_id VARCHAR(50),
    generation INTEGER,
    parent_id VARCHAR(50),
    mutation_signature VARCHAR(255),
    fitness_score FLOAT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS genome_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    organism_id INTEGER,
    genome_data JSON,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(organism_id) REFERENCES organism_lineage(id)
);
