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
    private final HttpClient client = HttpClient.newHttpClient();
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
            // POST /arena/{room_id}/register?robot_id={robot_id}
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
                    System.out.println("[INFO] Palavra-passe guardada com sucesso! -> " + this.palavraPasse);
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
            // CORREÇÃO SWAGGER: GET /arena/{room_id}/perceive/{robot_id}
            String url = BASE_URL + "/arena/" + roomId + "/perceive/" + codificar(robotId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String corpo = response.body();

            if (corpo == null || !corpo.trim().startsWith("{")) {
                System.out.println("[DEBUG] Erro no perceive. Servidor respondeu: " + corpo);
                return null;
            }

            return JsonParser.parseString(corpo).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Erro no perceive: " + e.getMessage());
            return null;
        }
    }

    public JsonObject agir(String acao) {
        try {
            // Rota limpa como pede o Swagger
            String url = BASE_URL + "/arena/action";

            // Criar o corpo JSON esperado pelo servidor FastAPI
            JsonObject jsonCorpo = new JsonObject();
            jsonCorpo.addProperty("room_id", this.roomId);   // <-- ADICIONADO AQUI! O servidor exige o ID da sala no body
            jsonCorpo.addProperty("robot_id", this.robotId);
            jsonCorpo.addProperty("action", acao.toLowerCase());
            String corpoString = jsonCorpo.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json") // Avisa o servidor que vai JSON no corpo
                    .POST(HttpRequest.BodyPublishers.ofString(corpoString))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String corpo = response.body();
            System.out.println("[DEBUG] Resposta do agir: " + corpo);

            if (corpo == null || !corpo.trim().startsWith("{")) {
                return null;
            }
            return JsonParser.parseString(corpo).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Erro no action: " + e.getMessage());
            return null;
        }
    }

    public JsonObject desbloquear(String code, String ragChunk, String llmRaw) {
        try {
            // POST /arena/{room_id}/unlock
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
            // GET /arena/{room_id}/download_manual
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