# Massive Array Parallel Processing in Java

![Java](https://img.shields.io/badge/Java-11%2B-ED8B00?style=for-the-badge&logo=java&logoColor=white) ![Multithreading](https://img.shields.io/badge/Concurrency-CompletableFuture-E34F26?style=for-the-badge)

This repository demonstrates advanced concurrency techniques in Java, focused on achieving maximum CPU utilization when processing massive datasets (arrays with over 100 million elements). 



The project benchmarks sequential logic against thread pools and explores modern asynchronous pipelines.

## 🚀 Key Features & Methodologies

* **Data Partitioning:** Dynamically splits large data structures into smaller, manageable blocks for parallel computation.
* **`ExecutorService` & Thread Pools:** Uses a fixed thread pool to schedule and manage hardware resources efficiently without the overhead of creating new threads for every task.
* **Thread-Safe Data Structures:** Utilizes `ArrayBlockingQueue` to safely aggregate partial results from multiple concurrent threads.
* **Modern Async Pipelines:** Implements `CompletableFuture` for non-blocking, reactive data processing.

## 🔍 Core Implementations

The project is split into two main approaches:

### 1. The Classic Thread Pool Approach (`Mean.java`)
Uses basic `Runnable` tasks submitted to an `ExecutorService`. Each worker thread calculates the mean of its assigned array chunk and pushes the result into an `ArrayBlockingQueue`. The main thread reads from this queue to compute the final aggregate mean.
* Includes a benchmarking loop that measures execution time based on the number of data chunks (1, 2, 4, 8, ... 128) to observe the point of diminishing returns in parallelization.

### 2. The Modern Async Approach (`AsyncMean.java`)
Uses `CompletableFuture` and `Supplier<Double>`. It showcases two aggregation strategies:
* **Version 1 (`join`):** Collects futures in a List and blocks the main thread sequentially via `.join()` to sum the results.
* **Version 2 (`thenApply`):** A reactive approach where each future is chained with a callback (`.thenApply`) that instantly pushes the computed result into a shared `BlockingQueue` as soon as it's ready, maximizing throughput.

## 🛠️ Technology Stack
* **Java 17+**
* **Concurrency APIs:** `java.util.concurrent.*` (`ExecutorService`, `CompletableFuture`, `ArrayBlockingQueue`, `Supplier`)

## 🧑‍💻 Author
Created as an advanced exploration of Java Multithreading patterns, highlighting the differences between legacy Thread/Runnable synchronization and modern functional/reactive asynchronous workflows.
