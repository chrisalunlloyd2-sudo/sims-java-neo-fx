import sqlite3
import random
import os
import logging
from datetime import datetime

# Attempt to import matplotlib for PNG graph rendering
try:
    import matplotlib.pyplot as plt
    import numpy as np
    MATPLOTLIB_AVAILABLE = True
except ImportError:
    MATPLOTLIB_AVAILABLE = False

logging.basicConfig(level=logging.INFO, format='%(asctime)s - [KAROO WRAPPER] - %(message)s')

class HexDBStore:
    def __init__(self, db_path="euler_hex_store.db"):
        self.db_path = db_path
        self.conn = sqlite3.connect(self.db_path)
        self.cursor = self.conn.cursor()
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS topological_memory (
                q INTEGER,
                r INTEGER,
                z_elevation INTEGER,
                generation INTEGER,
                fitness_score REAL,
                genome_signature TEXT,
                timestamp TEXT,
                PRIMARY KEY (q, r, z_elevation, generation)
            )
        ''')
        self.conn.commit()

    def insert_genetic_stat(self, q, r, z, generation, fitness, genome):
        self.cursor.execute('''
            INSERT OR REPLACE INTO topological_memory 
            (q, r, z_elevation, generation, fitness_score, genome_signature, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (q, r, z, generation, fitness, genome, datetime.now().isoformat()))
        self.conn.commit()

    def get_fitness_history(self, q, r, z):
        self.cursor.execute('''
            SELECT generation, fitness_score FROM topological_memory
            WHERE q=? AND r=? AND z_elevation=?
            ORDER BY generation ASC
        ''', (q, r, z))
        return self.cursor.fetchall()

def run_karoo_comparative_stats(hex_db, q, r, z, max_generations=50):
    logging.info(f"Initiating Karoo GP Genetic Analysis at Hex ({q},{r},{z})...")
    
    # Mocking Karoo GP evolutionary improvement
    base_fitness = random.uniform(0.1, 0.4)
    
    for gen in range(1, max_generations + 1):
        # Evolutionary progress (fitness increases over generations with some noise)
        fitness = min(0.99, base_fitness + (gen * random.uniform(0.005, 0.015)))
        genome = f"GP_NODE_{random.randint(1000,9999)}_{gen}"
        
        hex_db.insert_genetic_stat(q, r, z, gen, fitness, genome)
        
    logging.info(f"Evolution complete. Final Fitness: {fitness:.4f}")

def render_graph_png(hex_db, q, r, z, output_file="hex_vision_layer.png"):
    data = hex_db.get_fitness_history(q, r, z)
    if not data:
        logging.error("No data found to render.")
        return

    generations = [row[0] for row in data]
    fitness_scores = [row[1] for row in data]

    if MATPLOTLIB_AVAILABLE:
        plt.figure(figsize=(10, 6))
        plt.style.use('dark_background')
        
        plt.plot(generations, fitness_scores, marker='o', color='#00F2FE', linestyle='-', linewidth=2, markersize=4)
        plt.fill_between(generations, fitness_scores, color='#00F2FE', alpha=0.1)
        
        plt.title(f"Karoo GP Evolutionary Stats - Euler Hex ({q}, {r}, {z})", color='#FF00FF', fontsize=14)
        plt.xlabel("Generation", color='#00F2FE')
        plt.ylabel("Fitness Score", color='#00F2FE')
        
        plt.grid(color='#333333', linestyle='--', linewidth=0.5)
        
        plt.savefig(output_file, dpi=300, bbox_inches='tight', facecolor='#050505')
        plt.close()
        logging.info(f"Graph successfully rendered to {output_file}. Ready for Moondream vision orchestrator.")
    else:
        logging.warning("Matplotlib not installed. Generating mock ASCII representation instead of PNG.")
        with open(output_file.replace('.png', '.txt'), 'w') as f:
            f.write(f"Karoo GP Evolutionary Stats - Hex ({q},{r},{z})\n")
            for g, fit in zip(generations, fitness_scores):
                bar = "=" * int(fit * 50)
                f.write(f"Gen {g:03d} | {fit:.4f} | {bar}\n")
        logging.info(f"ASCII data layer saved to {output_file.replace('.png', '.txt')}")

if __name__ == "__main__":
    # Foundry Step Implementation
    store = HexDBStore("euler_hex_store.db")
    
    # 1. Run stats wrapper on a specific hex cell
    target_q, target_r, target_z = 0, 0, 0
    run_karoo_comparative_stats(store, target_q, target_r, target_z, max_generations=100)
    
    # 2. Render Graph PNG for Vision Model
    render_graph_png(store, target_q, target_r, target_z, output_file="karoo_hex_0_0_0_stats.png")
