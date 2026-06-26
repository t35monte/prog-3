import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== A INICIAR O CÉREBRO DO ROBÔ (TESTE DO ENGENHEIRO 2) ===");

        // 1. Instanciar o nosso cliente Ollama
        OllamaClient ollama = new OllamaClient();

        // 2. Simular um pequeno manual técnico (Chunking / Divisão por linhas)
        // Mais tarde, o teu colega vai descarregar o manual real do servidor da arena
        List<String> linhasDoManual = new ArrayList<>();
        linhasDoManual.add("O sistema de refrigeração hidráulica utiliza a válvula de segurança HYDRO-VALVE.");
        linhasDoManual.add("Em caso de falha crítica de energia térmica, insira o código de bypass THERM-99.");
        linhasDoManual.add("Os escudos magnéticos de liga leve respondem ao protocolo SHIELD-LOCK.");

        System.out.println("\n[1/4] A vetorizar o manual técnico local na memória RAM...");
        List<DocumentoVetorial> baseConhecimento = new ArrayList<>();

        for (String linha : linhasDoManual) {
            System.out.println("-> A gerar vetor para: \"" + linha + "\"");
            double[] vetorLinha = ollama.gerarEmbedding(linha);

            // Guardamos o texto e o seu vetor correspondente na nossa classe de dados
            baseConhecimento.add(new DocumentoVetorial(linha, vetorLinha));
        }
        System.out.println("Sucesso: Manual totalmente indexado!");

        // 3. Simular um enigma que o robô encontrou num cofre da arena
        String enigmaDoCofre = "Houve uma avaria térmica grave no gerador principal da fábrica!";
        System.out.println("\n[2/4] Enigma detetado no cofre: \"" + enigmaDoCofre + "\"");

        // Vetorizamos a pergunta/enigma
        double[] vetorEnigma = ollama.gerarEmbedding(enigmaDoCofre);

        // 4. Executar a Pesquisa Semântica (Cosine Similarity)
        System.out.println("\n[3/4] A calcular a semelhança de cossenos contra o manual...");
        DocumentoVetorial melhorParagrafo = null;
        double maiorSemelhanca = -1.0;

        for (DocumentoVetorial doc : baseConhecimento) {
            double semelhanca = ollama.calcularCosineSimilarity(vetorEnigma, doc.getVetor());
            System.out.printf("   Semelhança: %.4f para o texto: \"%s\"\n", semelhanca, doc.getTexto());

            if (semelhanca > maiorSemelhanca) {
                maiorSemelhanca = semelhanca;
                melhorParagrafo = doc;
            }
        }

        if (melhorParagrafo != null) {
            System.out.println("-> Parágrafo mais relevante encontrado: \"" + melhorParagrafo.getTexto() + "\"");

            // 5. Enviar o contexto correto para o LLM extrair a chave de forma determinística
            System.out.println("\n[4/4] A pedir ao modelo Qwen2.5 para extrair a chave isolada...");
            String chaveExtraida = ollama.extrairChaveComLLM(melhorParagrafo.getTexto(), enigmaDoCofre);

            System.out.println("\n=============================================");
            System.out.println("CHAVE FINAL GERADA PELO RAG: " + chaveExtraida);
            System.out.println("=============================================");
        } else {
            System.out.println("Erro: Não foi possível determinar o melhor parágrafo.");
        }
    }
}