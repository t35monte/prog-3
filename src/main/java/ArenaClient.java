import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ArenaClient {

    private static final String BASE_URL = "https://arena.pmonteiro.ovh";
    private final HttpClient client = HttpClient.newHttpClient();
    private final String roomId;
    private final String robotId;

    public ArenaClient(String roomId, String robotId) {
        this.roomId = roomId;
        this.robotId = robotId;
    }

    public String registar() {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/register?robot_id=" + robotId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Register: " + response.body());
            return response.body();
        } catch (Exception e) {
            System.out.println("Erro no register: " + e.getMessage());
            return null;
        }
    }

    public JsonObject percecionar() {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/perceive?robot_id=" + robotId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Erro no perceive: " + e.getMessage());
            return null;
        }
    }

    public JsonObject agir(String acao) {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/action?robot_id=" + robotId + "&action=" + acao;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Erro no action: " + e.getMessage());
            return null;
        }
    }

    public JsonObject desbloquear(String code, String ragChunk, String llmRaw) {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/unlock"
                    + "?robot_id=" + robotId
                    + "&code=" + code
                    + "&rag_chunk=" + ragChunk
                    + "&llm_raw=" + llmRaw;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Erro no unlock: " + e.getMessage());
            return null;
        }
    }

    public String descarregarManual() {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/download_manual";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            System.out.println("Erro no download_manual: " + e.getMessage());
            return null;
        }
    }
}