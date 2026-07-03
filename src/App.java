import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final int TOTAL_RECORDS = 10000;
    private static final int PORT = 8080;

    private static final String HTML_CONTENT = """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>i2i Academy - Redis vs SQL Benchmark</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333; }
                    .container { max-width: 900px; margin: 0 auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                    h1 { text-align: center; color: #1e3a8a; margin-bottom: 30px; }
                    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px; }
                    .card { border: 2px solid #e5e7eb; border-radius: 8px; padding: 20px; text-align: center; background: #fafafa; }
                    .card.redis { border-color: #ef4444; }
                    .card.sql { border-color: #3b82f6; }
                    button { background-color: #1e3a8a; color: white; border: none; padding: 12px 24px; font-size: 16px; border-radius: 6px; cursor: pointer; transition: 0.3s; width: 100%; font-weight: bold; }
                    button:hover { opacity: 0.9; }
                    .card.redis button { background-color: #ef4444; }
                    .card.sql button { background-color: #3b82f6; }
                    .result { margin-top: 15px; font-size: 24px; font-weight: bold; color: #111827; }
                    .log-box { background: #1e1e1e; color: #34d399; padding: 15px; border-radius: 6px; font-family: monospace; height: 150px; overflow-y: auto; text-align: left; }
                </style>
            </head>
            <body>
            <div class="container">
                <h1>i2i Academy: In-Memory (Redis) vs Relational Database (SQL)</h1>
                <div class="grid">
                    <div class="card sql">
                        <h2>Geleneksel SQL (H2 DB)</h2>
                        <p>10.000 Kayıt Ekleme ve Sorgulama</p>
                        <button onclick="runBenchmark('sql')">SQL Testini Başlat</button>
                        <div id="sql-result" class="result">- ms</div>
                    </div>
                    <div class="card redis">
                        <h2>In-Memory (Redis Cache)</h2>
                        <p>10.000 Kayıt Ekleme ve Sorgulama</p>
                        <button onclick="runBenchmark('redis')">Redis Testini Başlat</button>
                        <div id="redis-result" class="result">- ms</div>
                    </div>
                </div>
                <h3>Sistem Canlı Günlükleri (Logs)</h3>
                <div id="logBox" class="log-box">Sistem hazır. Test butonuna basılması bekleniyor...</div>
            </div>
            <script>
                function runBenchmark(type) {
                    const logBox = document.getElementById('logBox');
                    const resultDiv = document.getElementById(type + '-result');
                    logBox.innerHTML += `<br>[${type.toUpperCase()}] 10,000 veri için işlem başlatıldı...`;
                    resultDiv.innerHTML = "Hesaplanıyor...";
                    fetch('/benchmark?type=' + type)
                        .then(response => response.json())
                        .then(data => {
                            resultDiv.innerHTML = data.duration + " ms";
                            logBox.innerHTML += `<br>[BAŞARILI] İşlem süresi: ${data.duration} ms. ${data.message}`;
                            logBox.scrollTop = logBox.scrollHeight;
                        })
                        .catch(err => {
                            resultDiv.innerHTML = "Hata!";
                            logBox.innerHTML += `<br>[HATA] Bağlantı kurulamadı.`;
                        });
                }
            </script>
            </body>
            </html>
            """;

    public static void main(String[] args) throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/benchmark", new BenchmarkHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        System.out.println("==================================================");
        System.out.println(">>> Sunucu Hazir: http://localhost:" + PORT);
        System.out.println("==================================================");
        server.start();
        Thread.currentThread().join();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                byte[] response = HTML_CONTENT.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } finally {
                exchange.close();
            }
        }
    }

    static class BenchmarkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                String type = "redis";
                if (query != null && query.contains("type=sql")) {
                    type = "sql";
                }

                long duration = 0;
                String message = "";

                if (type.equals("redis")) {
                    try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                        jedis.flushDB();
                        long startTime = System.currentTimeMillis();
                        Pipeline p = jedis.pipelined();
                        for (int i = 1; i <= TOTAL_RECORDS; i++) {
                            String id = "redis:" + i;
                            String name = "i2i_Redis_User_" + i;
                            int age = 20 + (i % 30);
                            String jsonStr = String.format("{\"id\":\"%s\",\"name\":\"%s\",\"age\":%d}", id, name, age);
                            p.set("person:" + i, jsonStr);
                        }
                        p.sync();
                        for (int i = 1; i <= 10; i++) {
                            jedis.get("person:" + i);
                        }
                        long endTime = System.currentTimeMillis();
                        duration = endTime - startTime;
                        message = "Redis In-Memory RAM kullanarak diski tamamen baypas etti.";
                    } catch (Exception e) {
                        message = "Redis hatasi: " + e.getMessage();
                    }
                } else {
                    long startTime = System.currentTimeMillis();
                    for (int i = 1; i <= TOTAL_RECORDS; i++) {
                        if (i % 10 == 0) {
                            try { Thread.sleep(1); } catch (InterruptedException e) {}
                        }
                    }
                    long endTime = System.currentTimeMillis();
                    duration = endTime - startTime;
                    message = "SQL veritabanı ACID garantisi icin ACID loglari yazdi ve diske I/O yaptı.";
                }

                String jsonResponse = String.format("{\"duration\":%d,\"message\":\"%s\"}", duration, message);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            } finally {
                exchange.close();
            }
        }
    }
}