import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ArenaClient {

    private static final String BASE_URL = "https://arena.pmonteiro.ovh";
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final String roomId;
    private final String robotId;
    private String palavraPasse = "";

    public ArenaClient(String roomId, String robotId) {
        this.roomId = roomId;
        this.robotId = robotId;
    }

    private String codificar(String texto) {
        if (texto == null) return "";
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    public String registar() {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/register?robot_id=" + codificar(robotId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String corpo = response.body();
            System.out.println("Register Response: " + corpo);

            if (corpo != null && corpo.trim().startsWith("{")) {
                JsonObject json = JsonParser.parseString(corpo).getAsJsonObject();
                if (json.has("password")) {
                    this.palavraPasse = json.get("password").getAsString();
                    System.out.println("[INFO] Palavra-passe guardada: " + this.palavraPasse);
                }
            }
            return corpo;
        } catch (Exception e) {
            System.out.println("Erro no register: " + e.getMessage());
            return null;
        }
    }

    public JsonObject percecionar() {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/perceive/" + codificar(robotId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String corpo = response.body();
            System.out.println("[DEBUG] Perceive raw: " + corpo);

            if (corpo == null || corpo.isBlank()) return null;

            // Se vier como string JSON escapada (ex: "{\"x\":1}" com aspas externas), limpar
            String limpo = corpo.trim();
            if (limpo.startsWith("\"") && limpo.endsWith("\"")) {
                // Remover aspas externas e fazer unescape
                limpo = limpo.substring(1, limpo.length() - 1).replace("\\\"", "\"");
            }

            if (!limpo.startsWith("{")) {
                System.out.println("[DEBUG] Perceive não é JSON: " + limpo);
                return null;
            }

            return JsonParser.parseString(limpo).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Erro no perceive: " + e.getMessage());
            return null;
        }
    }

    public String agir(String acao) {

        try {

            String url = BASE_URL + "/arena/action";

            JsonObject body = new JsonObject();
            body.addProperty("room_id", roomId);
            body.addProperty("robot_id", robotId);
            body.addProperty("action", acao);

            System.out.println("====================================");
            System.out.println("POST " + url);
            System.out.println(body.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("HTTP: " + response.statusCode());
            System.out.println(response.body());
            System.out.println("====================================");

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JsonObject desbloquear(String code, String ragChunk, String llmRaw) {
        try {
            String url = BASE_URL + "/arena/" + roomId + "/unlock"
                    + "?robot_id=" + codificar(robotId)
                    + "&password=" + codificar(palavraPasse)
                    + "&code=" + codificar(code)
                    + "&rag_chunk=" + codificar(ragChunk)
                    + "&llm_raw=" + codificar(llmRaw);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String corpo = response.body();
            if (corpo == null || !corpo.trim().startsWith("{")) {
                return null;
            }
            return JsonParser.parseString(corpo).getAsJsonObject();
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