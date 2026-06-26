import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // ============================================
        // FASE DE ARRANQUE — ENGENHEIRO 2 (RAG)
        // ============================================
        System.out.println("=== A INICIAR O CÉREBRO DO ROBÔ ===");

        OllamaClient ollama = new OllamaClient();

        // Engenheiro 1 descarrega o manual real do servidor
        // e passa ao Engenheiro 2 para vetorizar
        ArenaClient clienteTemp = new ArenaClient("aluno_treino_2026", "RoboXPTO");
        String manualReal = clienteTemp.descarregarManual();

        // Por agora usa manual simulado se o servidor estiver fechado
        List<String> linhasDoManual = new ArrayList<>();
        if (manualReal != null && !manualReal.isEmpty()) {
            for (String linha : manualReal.split("\n")) {
                if (!linha.trim().isEmpty()) linhasDoManual.add(linha.trim());
            }
            System.out.println("Manual real descarregado do servidor!");
        } else {
            // Fallback para testes locais
            linhasDoManual.add("O sistema de refrigeração hidráulica utiliza a válvula de segurança HYDRO-VALVE.");
            linhasDoManual.add("Em caso de falha crítica de energia térmica, insira o código de bypass THERM-99.");
            linhasDoManual.add("Os escudos magnéticos de liga leve respondem ao protocolo SHIELD-LOCK.");
            System.out.println("Servidor fechado — a usar manual de teste local.");
        }

        // Vetorizar manual
        System.out.println("\n[RAG] A vetorizar manual na memória RAM...");
        List<DocumentoVetorial> baseConhecimento = new ArrayList<>();
        for (String linha : linhasDoManual) {
            double[] vetor = ollama.gerarEmbedding(linha);
            baseConhecimento.add(new DocumentoVetorial(linha, vetor));
        }
        System.out.println("[RAG] Manual indexado com " + baseConhecimento.size() + " blocos.");

        // ============================================
        // FASE PRINCIPAL — ENGENHEIRO 1 (AGENTE)
        // ============================================
        AgenteExplorador agente = new AgenteExplorador(baseConhecimento, ollama);
        agente.iniciar();
    }
}