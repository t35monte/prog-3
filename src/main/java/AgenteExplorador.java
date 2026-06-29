import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;

public class AgenteExplorador {

    private final ArenaClient arenaClient;
    private final Map<String, Integer> historicoVisitas = new HashMap<>();
    private final Set<String> paredesConhecidas = new HashSet<>();
    private final Map<String, Integer> contadorColisoes = new HashMap<>();
    private final Queue<String> filaAcoesPlaneadas = new LinkedList<>();

    private final GestorCofres gestorCofres;
    private final List<DocumentoVetorial> baseConhecimento;
    private final OllamaClient ollama;
    private final PainelMapaCalor painel;

    private int xAnterior = -1;
    private int yAnterior = -1;
    private String ultimaAcaoMovimento = null;
    private int turnosSemMover = 0;

    public AgenteExplorador(String roomId, String robotId, List<DocumentoVetorial> baseConhecimento, OllamaClient ollama) {
        this.arenaClient = new ArenaClient(roomId, robotId);
        this.baseConhecimento = baseConhecimento;
        this.ollama = ollama;
        this.gestorCofres = new GestorCofres();
        this.painel = PainelMapaCalor.criar();
    }

    public void iniciar() {
        arenaClient.registar();

        while (true) {
            try {
                JsonObject telemetria = arenaClient.percecionar();
                if (telemetria != null) {
                    System.out.println("[TELEMETRIA] " + telemetria.toString());
                }
                if (telemetria == null) {
                    Thread.sleep(400);
                    continue;
                }

                boolean jogoIniciado = telemetria.has("game_started") && telemetria.get("game_started").getAsBoolean();
                if (!jogoIniciado) {
                    System.out.println("[AGUARDAR] Jogo ainda não iniciado...");
                    Thread.sleep(1000);
                    continue;
                }

                boolean jogoTerminado = telemetria.has("game_over") && telemetria.get("game_over").getAsBoolean();
                if (jogoTerminado) {
                    System.out.println("[FIM] Jogo terminado!");
                    break;
                }

                JsonObject estado = null;
                if (telemetria.has("estado")) {
                    estado = telemetria.getAsJsonObject("estado");
                } else if (telemetria.has("o_meu_estado")) {
                    estado = telemetria.getAsJsonObject("o_meu_estado");
                }

                if (estado == null) {
                    Thread.sleep(400);
                    continue;
                }

                int x = estado.has("x") ? (int) estado.get("x").getAsDouble() : 0;
                int y = estado.has("y") ? (int) estado.get("y").getAsDouble() : 0;
                int hp = estado.has("energia") ? (int) estado.get("energia").getAsDouble() :
                        (estado.has("hp") ? (int) estado.get("hp").getAsDouble() : 200);

                System.out.println("Posição Atual: (" + x + ", " + y + ") | Energia/HP: " + hp);

                // Deteção de colisão com confirmação (2 tentativas antes de marcar como parede)
                if (ultimaAcaoMovimento != null && xAnterior >= 0) {
                    if (x == xAnterior && y == yAnterior) {
                        turnosSemMover++;
                        int alvox = xAnterior;
                        int alvoy = yAnterior;
                        switch (ultimaAcaoMovimento) {
                            case "NORTE": alvoy -= 1; break;
                            case "SUL":   alvoy += 1; break;
                            case "ESTE":  alvox += 1; break;
                            case "OESTE": alvox -= 1; break;
                        }
                        String coordAlvo = alvox + "," + alvoy;
                        int tentativas = contadorColisoes.getOrDefault(coordAlvo, 0) + 1;
                        contadorColisoes.put(coordAlvo, tentativas);

                        if (tentativas >= 2) {
                            System.out.println("[PAREDE CONFIRMADA] " + coordAlvo);
                            paredesConhecidas.add(coordAlvo);
                        } else {
                            System.out.println("[COLISÃO #" + tentativas + "] " + ultimaAcaoMovimento + " -> " + coordAlvo);
                        }

                        // Se está preso há muitos turnos, resetar tudo e tentar escapar
                        if (turnosSemMover >= 6) {
                            System.out.println("[RESET] Preso há " + turnosSemMover + " turnos! A resetar paredes.");
                            paredesConhecidas.clear();
                            contadorColisoes.clear();
                            filaAcoesPlaneadas.clear();
                            turnosSemMover = 0;
                            // Adicionar sequência de escape aleatória
                            String[] escape = {"NORTE", "ESTE", "SUL", "OESTE", "NORTE", "ESTE"};
                            for (String dir : escape) filaAcoesPlaneadas.add(dir);
                        }
                    } else {
                        // Moveu com sucesso
                        turnosSemMover = 0;
                        contadorColisoes.clear();
                    }
                }
                xAnterior = x;
                yAnterior = y;
                ultimaAcaoMovimento = null;

                painel.atualizar(historicoVisitas, x, y, hp, "");

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

                    if (acao.equals("NORTE") || acao.equals("SUL") ||
                            acao.equals("ESTE") || acao.equals("OESTE")) {

                        ultimaAcaoMovimento = acao;
                    }

                    String acaoAPI;

                    switch (acao) {
                        case "NORTE":
                            acaoAPI = "MOVER_NORTE";
                            break;

                        case "SUL":
                            acaoAPI = "MOVER_SUL";
                            break;

                        case "ESTE":
                            acaoAPI = "MOVER_ESTE";
                            break;

                        case "OESTE":
                            acaoAPI = "MOVER_OESTE";
                            break;

                        default:
                            acaoAPI = acao;
                            break;
                    }

                    arenaClient.agir(acaoAPI);
                }

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
            if (telemetria.has("cofres_no_mundo") && !telemetria.get("cofres_no_mundo").isJsonNull()) {
                JsonArray cofres = telemetria.getAsJsonArray("cofres_no_mundo");
                for (JsonElement el : cofres) {
                    JsonObject cofre = el.getAsJsonObject();
                    int cx = (int) cofre.get("x").getAsDouble();
                    int cy = (int) cofre.get("y").getAsDouble();
                    if (cx == x && cy == y && cofre.has("terminal_desafio") && !cofre.get("terminal_desafio").isJsonNull()) {
                        String enigma = cofre.get("terminal_desafio").getAsString();
                        if (!enigma.isEmpty() && gestorCofres.podeInteragir(x, y)) {
                            System.out.println("Cofre detetado em (" + x + "," + y + ")! Enigma: " + enigma);
                            String chave = ollama.resolverDesafioRAG(enigma, baseConhecimento);
                            System.out.println("Chave extraída pelo RAG: " + chave);
                            JsonObject resultado = arenaClient.desbloquear(chave, "Contexto RAG enviado pelo Agente", chave);
                            if (resultado != null && resultado.has("status")) {
                                String status = resultado.get("status").getAsString();
                                if (status.equalsIgnoreCase("sucesso") || status.equalsIgnoreCase("registado")) {
                                    gestorCofres.registarSucesso(x, y);
                                } else {
                                    gestorCofres.registarFalhaNaListaNegra(x, y);
                                }
                            }
                            return null;
                        }
                    }
                }
            }

            if (!telemetria.has("terminal_desafio") || telemetria.get("terminal_desafio").isJsonNull()) return null;
            String enigma = telemetria.get("terminal_desafio").getAsString();
            if (enigma.isEmpty() || !gestorCofres.podeInteragir(x, y)) return null;

            System.out.println("Cofre detetado em (" + x + "," + y + ")! Enigma: " + enigma);
            String chave = ollama.resolverDesafioRAG(enigma, baseConhecimento);
            System.out.println("Chave extraída pelo RAG: " + chave);
            JsonObject resultado = arenaClient.desbloquear(chave, "Contexto RAG enviado pelo Agente", chave);
            if (resultado != null && resultado.has("status")) {
                String status = resultado.get("status").getAsString();
                if (status.equalsIgnoreCase("sucesso") || status.equalsIgnoreCase("registado")) {
                    gestorCofres.registarSucesso(x, y);
                } else {
                    gestorCofres.registarFalhaNaListaNegra(x, y);
                }
            }
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
                for (JsonElement el : elementRivais.getAsJsonArray()) {
                    String a = processarRivalIndividual(el.getAsJsonObject(), x, y, meuHp);
                    if (a != null) return a;
                }
            } else if (elementRivais.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : elementRivais.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        String a = processarRivalIndividual(entry.getValue().getAsJsonObject(), x, y, meuHp);
                        if (a != null) return a;
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
                String fuga = moverOpostoA(x, y, rx, ry);
                filaAcoesPlaneadas.add(fuga);
                filaAcoesPlaneadas.add(fuga);
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

            if (paredesConhecidas.contains(coordVizinho)) {
                System.out.println("[MOVIMENTO] " + direcoes[i] + " -> (" + nx + "," + ny + ") parede=true (colisão) calor=0");
                continue;
            }

            boolean parede = temParede(paredesArray, nx, ny);
            int calor = historicoVisitas.getOrDefault(coordVizinho, 0);
            System.out.println("[MOVIMENTO] " + direcoes[i] + " -> (" + nx + "," + ny + ") parede=" + parede + " calor=" + calor);

            if (parede) continue;

            if (calor < menorCalor) {
                menorCalor = calor;
                melhorDirecao = direcoes[i];
            }
        }

        if (melhorDirecao == null) {
            System.out.println("[EMERGÊNCIA] Todas as direções bloqueadas! A limpar memória de paredes.");
            paredesConhecidas.clear();
            contadorColisoes.clear();
            for (int i = 0; i < direcoes.length; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];
                if (!temParede(paredesArray, nx, ny)) {
                    int calor = historicoVisitas.getOrDefault(nx + "," + ny, 0);
                    if (calor < menorCalor) {
                        menorCalor = calor;
                        melhorDirecao = direcoes[i];
                    }
                }
            }
            if (melhorDirecao == null) melhorDirecao = "NORTE";
        }

        System.out.println("[ESCOLHA] " + melhorDirecao);
        return melhorDirecao;
    }

    private boolean temParede(JsonArray objetos, int x, int y) {
        if (objetos == null) return false;
        for (JsonElement el : objetos) {
            JsonObject obj = el.getAsJsonObject();
            int ox = (int) obj.get("x").getAsDouble();
            int oy = (int) obj.get("y").getAsDouble();
            if (ox == x && oy == y) return true;
        }
        return false;
    }

    private String moverEmDirecao(int x, int y, int tx, int ty) {
        if (Math.abs(tx - x) > Math.abs(ty - y)) return tx > x ? "ESTE" : "OESTE";
        return ty > y ? "SUL" : "NORTE";
    }

    private String moverOpostoA(int x, int y, int rx, int ry) {
        if (Math.abs(rx - x) > Math.abs(ry - y)) return rx > x ? "OESTE" : "ESTE";
        return ry > y ? "NORTE" : "SUL";
    }
}