import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.List;

public class AgenteExplorador {

    private static final String ROOM_ID = "aluno_treino_2026";
    private static final String ROBOT_ID = "RoboXPTO";

    // DEPOIS (cola isto):
    private final ArenaClient arenaClient;
    private final Map<String, Integer> historicoVisitas = new HashMap<>();
    private final Queue<String> filaAcoesPlaneadas = new LinkedList<>();
    private final List<DocumentoVetorial> baseConhecimento;
    private final OllamaClient ollama;

    public AgenteExplorador(List<DocumentoVetorial> baseConhecimento, OllamaClient ollama) {
        this.arenaClient = new ArenaClient(ROOM_ID, ROBOT_ID);
        this.baseConhecimento = baseConhecimento;
        this.ollama = ollama;
    }

    public void iniciar() {
        // Registo na arena
        arenaClient.registar();

        // Ciclo principal Sense-Think-Act
        while (true) {
            try {
                // === SENSE ===
                JsonObject telemetria = arenaClient.percecionar();
                if (telemetria == null) {
                    Thread.sleep(400);
                    continue;
                }

                // Extrair estado do robô
                JsonObject estado = telemetria.getAsJsonObject("o_meu_estado");
                int x = estado.get("x").getAsInt();
                int y = estado.get("y").getAsInt();
                int hp = estado.get("hp").getAsInt();
                System.out.println("Posição: (" + x + ", " + y + ") | HP: " + hp);

                // Registar visita no mapa de calor
                String coordenada = x + "," + y;
                historicoVisitas.put(coordenada, historicoVisitas.getOrDefault(coordenada, 0) + 1);

                // === THINK ===
                String acao;

                // Se há ações planeadas (fuga, etc), executar primeiro
                if (!filaAcoesPlaneadas.isEmpty()) {
                    acao = filaAcoesPlaneadas.poll();
                } else {
                    // Verificar se há cofre na posição atual
                    acao = verificarCofre(telemetria);

                    if (acao == null) {
                        // Verificar rivais e decidir fight or flight
                        acao = verificarRivais(telemetria, x, y, hp);
                    }

                    if (acao == null) {
                        // Navegação normal — mapa de calor
                        acao = escolherMelhorMovimento(telemetria, x, y);
                    }
                }

                // === ACT ===
                arenaClient.agir(acao);

                // Anti-Flood obrigatório
                Thread.sleep(375);

            } catch (InterruptedException e) {
                System.out.println("Agente interrompido.");
                break;
            }
        }
    }

    private String verificarCofre(JsonObject telemetria) {
        // TODO: integração com Eng 2
        // Quando o Eng 2 tiver o motorRAG pronto:
        // JsonArray cofres = telemetria.getAsJsonArray("cofres_no_mundo");
        // se houver cofre na posição atual, chamar motorRAG.resolverEnigma()
        return null;
    }

    private String verificarRivais(JsonObject telemetria, int x, int y, int meuHp) {
        try {
            JsonArray rivais = telemetria.getAsJsonArray("outros_robots");
            if (rivais == null || rivais.size() == 0) return null;

            for (JsonElement el : rivais) {
                JsonObject rival = el.getAsJsonObject();
                int rx = rival.get("x").getAsInt();
                int ry = rival.get("y").getAsInt();
                int rivalHp = rival.get("hp").getAsInt();

                int distancia = Math.abs(x - rx) + Math.abs(y - ry);

                if (distancia <= 2) {
                    if (meuHp > rivalHp) {
                        // Fight — mover em direção ao rival
                        return moverEmDirecao(x, y, rx, ry);
                    } else {
                        // Flight — injetar movimentos de fuga na fila
                        filaAcoesPlaneadas.add(moverOpostoA(x, y, rx, ry));
                        filaAcoesPlaneadas.add(moverOpostoA(x, y, rx, ry));
                        return filaAcoesPlaneadas.poll();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao verificar rivais: " + e.getMessage());
        }
        return null;
    }

    private String escolherMelhorMovimento(JsonObject telemetria, int x, int y) {
        // Direções possíveis
        String[] direcoes = {"MOVER_NORTE", "MOVER_SUL", "MOVER_ESTE", "MOVER_OESTE"};
        int[] dx = {0, 0, 1, -1};
        int[] dy = {-1, 1, 0, 0};

        // Obter paredes
        JsonArray paredesArray = telemetria.getAsJsonArray("objetos_fixos");

        String melhorDirecao = null;
        int menorCalor = Integer.MAX_VALUE;

        for (int i = 0; i < direcoes.length; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            // Verificar se há parede nessa direção
            if (temParede(paredesArray, nx, ny)) continue;

            // Verificar calor da célula
            int calor = historicoVisitas.getOrDefault(nx + "," + ny, 0);
            if (calor < menorCalor) {
                menorCalor = calor;
                melhorDirecao = direcoes[i];
            }
        }

        // Se não encontrou nenhuma direção livre, tenta qualquer uma
        return melhorDirecao != null ? melhorDirecao : "MOVER_NORTE";
    }

    private boolean temParede(JsonArray objetos, int x, int y) {
        if (objetos == null) return false;
        for (JsonElement el : objetos) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.get("x").getAsInt() == x && obj.get("y").getAsInt() == y) {
                return true;
            }
        }
        return false;
    }

    private String moverEmDirecao(int x, int y, int tx, int ty) {
        if (Math.abs(tx - x) > Math.abs(ty - y)) {
            return tx > x ? "MOVER_ESTE" : "MOVER_OESTE";
        } else {
            return ty > y ? "MOVER_SUL" : "MOVER_NORTE";
        }
    }

    private String moverOpostoA(int x, int y, int rx, int ry) {
        if (Math.abs(rx - x) > Math.abs(ry - y)) {
            return rx > x ? "MOVER_OESTE" : "MOVER_ESTE";
        } else {
            return ry > y ? "MOVER_NORTE" : "MOVER_SUL";
        }
    }
}