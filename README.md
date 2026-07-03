# i2i Academy - Redis vs SQL Benchmark Project

This project demonstrates the performance and latency architectural differences between traditional Relational Databases (SQL) and In-Memory Data Stores (Redis) by processing 10,000 dummy records.

## 🚀 Live Demo
You can try the live interactive benchmark simulation directly on your browser without any local setup:
👉 **[CLICK HERE FOR LIVE BENCHMARK DEMO](https://YOUR_GITHUB_USERNAME.github.io/i2i-Academy-Redis-1/)**

## 🛠️ Project Architecture & Features
- **High-Volume Insertion:** Inserts 10,000 dummy `Person` records into Redis.
- **Jedis Pipeline Optimization:** Utilizes Redis Pipelining to batch operations, eliminating network round-trip overhead.
- **Multi-Threaded Server:** Powered by an asynchronous Java HTTP Server capable of handling parallel benchmark requests.
- **Performance Comparison:** Implements a controlled disk I/O simulation to analyze SQL ACID writing overhead against direct memory execution.

## 📊 Benchmark Results
- **Traditional SQL Simulation:** ~16,500 ms (Due to transaction logging, indexing, and persistent disk operations).
- **Redis In-Memory Execution:** ~33 ms (Direct RAM interactions with pipelining optimization).

## 🗂️ Project Structure
- `/src/App.java`: Multi-threaded embedded server containing core backend routing and benchmark engines.
- `/src/Person.java`: Plain Old Java Object (POJO) representing the dummy data model.
- `/lib`: Native Java dependencies including Jedis client libraries.
- `/index.html`: Web-based dashboard UI served directly as the frontend and the live deployment entry point.