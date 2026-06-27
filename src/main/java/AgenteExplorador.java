import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import java.util.Set;

public class AgenteExplorador {

    private final ArenaClient arenaClient;
    private final Map<String, Integer> historicoVisitas = new HashMap<>();

    // CORREÇÃO: Guarda as coordenadas que temos a certeza absoluta que são paredes/obstáculos
    private final Set<String> paredesConhecidas = new HashSet<>();

    private final Queue<String> filaAcoesPlaneadas = new LinkedList<>();

    private final GestorCofres gestorCofres;
    private final List<DocumentoVetorial> baseConhecimento;
    private final OllamaClient ollama;
    private final PainelMapaCalor painel;

    public AgenteExplorador(String roomId, String robotId, List<DocumentoVetorial> baseConhecimento, OllamaClient ollama) {
        this.arenaClient = new ArenaClient(roomId, robotId);
        this.baseConhecimento = baseConhecimento;
        this.ollama = ollama;
        this.gestorCofres = new GestorCofres();
        this.painel = PainelMapaCalor.criar();
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

                // Identificar o nó do estado do robô
                JsonObject estado = null;
                if (telemetria.has("estado")) {
                    estado = telemetria.getAsJsonObject("estado");
                } else if (telemetria.has("o_meu_estado")) {
                    estado = telemetria.getAsJsonObject("o_meu_estado");
                }

                if (estado == null) {
                    System.out.println("[AVISO] Não foi possível encontrar o estado do robô na telemetria.");
                    Thread.sleep(400);
                    continue;
                }

                int x = estado.has("x") ? (int) estado.get("x").getAsDouble() : 0;
                int y = estado.has("y") ? (int) estado.get("y").getAsDouble() : 0;
                int hp = estado.has("energia") ? (int) estado.get("energia").getAsDouble() :
                        (estado.has("hp") ? (int) estado.get("hp").getAsDouble() : 200);

                System.out.println("Posição Atual: (" + x + ", " + y + ") | Energia/HP: " + hp);

                // Atualizar o painel visual
                painel.atualizar(historicoVisitas, x, y, hp, "");

                // Registar visita no mapa de calor
                String coordenadaAtual = x + "," + y;
                historicoVisitas.put(coordenadaAtual, historicoVisitas.getOrDefault(coordenadaAtual, 0) + 1);

                // === THINK ===
                String acao = null;

                if (!filaAcoesPlaneadas.isEmpty()) {
                    acao = filaAcoesPlaneadas.poll();
                }

                if (acao == null) {
                    acao = verificarCofre(telemetria, x, y);
                }

                if (acao == null) {
                    acao = verificarRivais(telemetria, x, y, hp);
                }

                if (acao == null) {
                    acao = escolherMelhorMovimento(telemetria, x, y);
                }

                // === ACT ===
                if (acao != null) {
                    System.out.println("A enviar ação para o servidor: " + acao);

                    JsonObject respostaAcao = arenaClient.agir(acao);

                    if (respostaAcao != null && respostaAcao.has("status")) {
                        String status = respostaAcao.get("status").getAsString();

                        // Se o servidor diz que estamos bloqueados por falta de início ou pausa
                        if (status.equalsIgnoreCase("bloqueado")) {
                            String motivo = respostaAcao.has("motivo") ? respostaAcao.get("motivo").getAsString() : "";
                            System.out.println("[SERVIDOR BLOQUEADO] Não é possível mover. Motivo: " + motivo);

                            // Não fazemos nada, não guardamos parede, apenas esperamos o próximo turno
                        }
                        // SÓ processa colisão se o comando foi processado com SUCESSO mas a posição não mudou
                        else if (status.equalsIgnoreCase("sucesso") &&
                                (acao.equals("NORTE") || acao.equals("SUL") || acao.equals("ESTE") || acao.equals("OESTE"))) {

                            if (respostaAcao.has("nova_posicao")) {
                                JsonObject nPos = respostaAcao.getAsJsonObject("nova_posicao");
                                int nx = (int) nPos.get("x").getAsDouble();
                                int ny = (int) nPos.get("y").getAsDouble();

                                if (nx == x && ny == y) {
                                    int alvox = x + (acao.equals("ESTE") ? 1 : acao.equals("OESTE") ? -1 : 0);
                                    int alvoy = y + (acao.equals("SUL") ? 1 : acao.equals("NORTE") ? -1 : 0);
                                    String coordAlvo = alvox + "," + alvoy;

                                    System.out.println("[PAREDE REAL DETETADA] Bloqueado a " + acao + ". Adicionado à lista negra: " + coordAlvo);
                                    paredesConhecidas.add(coordAlvo);
                                }
                            }
                        }
                    }
                }

                // Anti-Flood obrigatório
                Thread.sleep(375);

            } catch (InterruptedException e) {
                System.out.println("Agente interrompido.");
                break;
            } catch (Exception e) {
                System.out.println("[ERRO NO LOOP]: " + e.getMessage());
                e.printStackTrace();
                try { Thread.sleep(1000); } catch (Exception ignored) {}
            }
        }
    }

    private String verificarCofre(JsonObject telemetria, int x, int y) {
        try {
            if (!telemetria.has("terminal_desafio") || telemetria.get("terminal_desafio").isJsonNull()) {
                return null;
            }

            String enigma = telemetria.get("terminal_desafio").getAsString();
            if (enigma.isEmpty()) return null;

            if (!gestorCofres.podeInteragir(x, y)) {
                return null;
            }

            System.out.println("Cofre detetado em ("+x+","+y+")! Enigma: " + enigma);

            String chave = ollama.resolverDesafioRAG(enigma, baseConhecimento);
            System.out.println("Chave extraída pelo RAG: " + chave);

            // Submissão da chave
            JsonObject resultado = arenaClient.desbloquear(chave, "Contexto RAG enviado pelo Agente", chave);

            if (resultado != null && resultado.has("status")) {
                String status = resultado.get("status").getAsString();
                if (status.equalsIgnoreCase("sucesso") || status.equalsIgnoreCase("registado")) {
                    gestorCofres.registarSucesso(x, y);
                } else {
                    gestorCofres.registarFalhaNaListaNegra(x, y);
                }
            }

            return "ESPERAR";

        } catch (Exception e) {
            System.out.println("Erro ao verificar cofre: " + e.getMessage());
        }
        return null;
    }

    private String verificarRivais(JsonObject telemetria, int x, int y, int meuHp) {
        try {
            if (!telemetria.has("outros_robots") || telemetria.get("outros_robots").isJsonNull()) return null;

            JsonElement elementRivais = telemetria.get("outros_robots");

            if (elementRivais.isJsonArray()) {
                JsonArray rivais = elementRivais.getAsJsonArray();
                for (JsonElement el : rivais) {
                    String acaoFuga = processarRivalIndividual(el.getAsJsonObject(), x, y, meuHp);
                    if (acaoFuga != null) return acaoFuga;
                }
            }
            else if (elementRivais.isJsonObject()) {
                JsonObject rivaisObj = elementRivais.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : rivaisObj.entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        String acaoFuga = processarRivalIndividual(entry.getValue().getAsJsonObject(), x, y, meuHp);
                        if (acaoFuga != null) return acaoFuga;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao verificar rivais: " + e.getMessage());
        }
        return null;
    }

    private String processarRivalIndividual(JsonObject rival, int x, int y, int meuHp) {
        if (!rival.has("x") || !rival.has("y")) return null;

        int rx = (int) rival.get("x").getAsDouble();
        int ry = (int) rival.get("y").getAsDouble();
        int rivalHp = rival.has("energia") ? (int) rival.get("energia").getAsDouble() :
                (rival.has("hp") ? (int) rival.get("hp").getAsDouble() : 100);

        int distancia = Math.abs(x - rx) + Math.abs(y - ry);

        if (distancia <= 2) {
            if (meuHp > rivalHp) {
                System.out.println("[COMBATE] A atacar rival em (" + rx + "," + ry + ")");
                return moverEmDirecao(x, y, rx, ry);
            } else {
                System.out.println("[FUGA] A fugir do rival em (" + rx + "," + ry + ")");
                filaAcoesPlaneadas.add(moverOpostoA(x, y, rx, ry));
                filaAcoesPlaneadas.add(moverOpostoA(x, y, rx, ry));
                return filaAcoesPlaneadas.poll();
            }
        }
        return null;
    }

    private String escolherMelhorMovimento(JsonObject telemetria, int x, int y) {
        String[] direcoes = {"NORTE", "SUL", "ESTE", "OESTE"};
        int[] dx = {0, 0, 1, -1};
        int[] dy = {-1, 1, 0, 0};

        JsonArray paredesArray = telemetria.has("objetos_fixos") ? telemetria.getAsJsonArray("objetos_fixos") : null;

        String melhorDirecao = null;
        int menorCalor = Integer.MAX_VALUE;

        for (int i = 0; i < direcoes.length; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            String coordVizinho = nx + "," + ny;

            // CORREÇÃO 1: Se esta coordenada está na nossa lista negra de colisão, ignora por completo
            if (paredesConhecidas.contains(coordVizinho)) {
                continue;
            }

            // Se bater numa parede estática vinda da simulação, ignora a direção
            if (temParede(paredesArray, nx, ny)) {
                continue;
            }

            int calor = historicoVisitas.getOrDefault(coordVizinho, 0);
            if (calor < menorCalor) {
                menorCalor = calor;
                melhorDirecao = direcoes[i];
            }
        }

        // CORREÇÃO 2: Se o robô ficar sem saídas válidas (ex: todas as 4 direções falharam)
        if (melhorDirecao == null) {
            System.out.println("[EMERGÊNCIA] Sem direções livres! Limpar paredesConhecidas para reavaliar.");
            paredesConhecidas.clear();
            return "NORTE"; // Movimento de escape padrão
        }

        return melhorDirecao;
    }

    private boolean temParede(JsonArray objetos, int x, int y) {
        if (objetos == null) return false;
        for (JsonElement el : objetos) {
            JsonObject obj = el.getAsJsonObject();
            int ox = (int) obj.get("x").getAsDouble();
            int oy = (int) obj.get("y").getAsDouble();
            if (ox == x && oy == y) {
                return true;
            }
        }
        return false;
    }

    private String moverEmDirecao(int x, int y, int tx, int ty) {
        if (Math.abs(tx - x) > Math.abs(ty - y)) {
            return tx > x ? "ESTE" : "OESTE";
        } else {
            return ty > y ? "SUL" : "NORTE";
        }
    }

    private String moverOpostoA(int x, int y, int rx, int ry) {
        if (Math.abs(rx - x) > Math.abs(ry - y)) {
            return rx > x ? "OESTE" : "ESTE";
        } else {
            return ry > y ? "NORTE" : "SUL";
        }
    }
}