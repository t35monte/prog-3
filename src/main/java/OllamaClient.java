import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class OllamaClient {

    private final HttpClient httpClient;
    private final Gson gson;
    private final String OLLAMA_HOST = "http://localhost:11434";

    public OllamaClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * 4.1. MÉTODO DE VETORIZAÇÃO (EMBEDDINGS)
     * Envia um texto para o Ollama e recebe um array de double[] (o vetor)
     */
    public double[] gerarEmbedding(String texto) {
        try {
            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("model", "nomic-embed-text");
            jsonBody.addProperty("prompt", texto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_HOST + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(jsonBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            JsonArray embeddingJsonArray = jsonResponse.getAsJsonArray("embedding");

            double[] vetor = new double[embeddingJsonArray.size()];
            for (int i = 0; i < embeddingJsonArray.size(); i++) {
                vetor[i] = embeddingJsonArray.get(i).getAsDouble();
            }
            return vetor;

        } catch (Exception e) {
            System.err.println("Erro ao gerar embedding: " + e.getMessage());
            return new double[0];
        }
    }

    /**
     * 4.2. MÉTODO MATEMÁTICO (COSINE SIMILARITY)
     * Calcula a semelhança entre dois vetores. Retorna um valor entre 0.0 e 1.0
     */
    public double calcularCosineSimilarity(double[] vetorA, double[] vetorB) {
        if (vetorA.length != vetorB.length || vetorA.length == 0) return 0.0;

        double produtoEscalar = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (int i = 0; i < vetorA.length; i++) {
            produtoEscalar += vetorA[i] * vetorB[i];
            normaA += Math.pow(vetorA[i], 2);
            normaB += Math.pow(vetorB[i], 2);
        }

        return produtoEscalar / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }

    /**
     * 4.3. MÉTODO DE EXTRAÇÃO LLM
     * Envia o contexto (parágrafo correto) e o enigma do cofre, e devolve APENAS a chave.
     */
    public String extrairChaveComLLM(String contexto, String enigma) {
        try {
            // Construção do Prompt estrito usando o formato ChatML pedido no enunciado
            String promptChatML =
                    "<|im_start|>system\n" +
                            "És um extrator de dados industrial estrito. O teu único trabalho é extrair a chave alfanumérica do manual que resolve o enigma. Responde APENAS com a chave isolada. Não inventes nem uses frases.\n" +
                            "<|im_end|>\n" +
                            "<|im_start|>user\n" +
                            "Manual: " + contexto + "\n" +
                            "Enigma: " + enigma + "\n" +
                            "<|im_end|>\n" +
                            "<|im_start|>assistant\n";

            JsonObject options = new JsonObject();
            options.addProperty("temperature", 0.0); // Remove a criatividade da IA

            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("model", "qwen2.5-coder:0.5b-instruct-q4_K_M");
            jsonBody.addProperty("prompt", promptChatML);
            jsonBody.addProperty("stream", false); // Queremos a resposta de uma só vez
            jsonBody.add("options", options);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_HOST + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(jsonBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            return jsonResponse.get("response").getAsString().trim();

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com LLM: " + e.getMessage());
            return "ERRO";
        }
    }
}
