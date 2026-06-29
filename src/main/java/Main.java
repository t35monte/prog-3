import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // CONFIGURAÇÃO CENTRAL (Muda apenas aqui no dia do exame!)
        String salaFinal = "8E37DA";
        String nomeDoRobo = "oi";

        System.out.println("=== A INICIAR O CÉREBRO DO ROBÔ ===");
        OllamaClient ollama = new OllamaClient();

        // Usa as variáveis centralizadas para descarregar o manual
        ArenaClient clienteTemp = new ArenaClient(salaFinal, nomeDoRobo);
        String manualReal = clienteTemp.descarregarManual();

        List<String> linhasDoManual = new ArrayList<>();
        if (manualReal != null && !manualReal.isEmpty()) {
            for (String linha : manualReal.split("\n")) {
                if (!linha.trim().isEmpty()) linhasDoManual.add(linha.trim());
            }
            System.out.println("Manual real descarregado do servidor!");
        } else {
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

        // Inicializa o agente passando as variáveis da sala e do robô
        // DEPOIS:
        // ALTERA ESTA LINHA:
        AgenteExplorador agente = new AgenteExplorador(salaFinal, nomeDoRobo, baseConhecimento, ollama);
        agente.iniciar();
    }
}