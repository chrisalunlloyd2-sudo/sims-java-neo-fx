-- Step 2: Design hypertable for events/chunks
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS vector; -- pgvector support

-- Define the core RAG memory architecture
CREATE TABLE rag_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ts TIMESTAMPTZ NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    source VARCHAR(255),
    chunk_text TEXT,
    embedding vector(768), -- Assumes Nomic-embed-text (768 dimensions)
    metadata JSONB
);

-- Convert standard table into a high-performance Hypertable (Time-Partitioned)
SELECT create_hypertable('rag_events', 'ts');

-- Step 3: Create time-partitioning + compression policies
-- Compress older chunks based on session_id groupings
ALTER TABLE rag_events SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'session_id'
);
-- Auto-compress chunks older than 30 days
SELECT add_compression_policy('rag_events', INTERVAL '30 days');

-- Auto-prune data older than 1 year (Retention Horizon)
SELECT add_retention_policy('rag_events', INTERVAL '365 days');

-- Step 5: Add indexes for RAG access paths
CREATE INDEX ix_rag_events_session ON rag_events USING btree(session_id);
CREATE INDEX ix_rag_events_source ON rag_events USING btree(source);
CREATE INDEX ix_rag_events_ts ON rag_events USING btree(ts DESC);
CREATE INDEX ix_rag_events_metadata ON rag_events USING gin(metadata);

-- HNSW Vector Index for blazing fast semantic similarity search
CREATE INDEX ix_rag_events_embedding ON rag_events USING hnsw (embedding vector_cosine_ops);
