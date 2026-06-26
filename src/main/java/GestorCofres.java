import java.util.HashSet;
import java.util.Set;

public class GestorCofres {

    // Guarda as coordenadas dos cofres que já foram abertos com sucesso
    private final Set<String> cofresResolvidos;

    // Guarda as coordenadas dos cofres onde a IA falhou (Lista Negra)
    private final Set<String> listaNegraCofres;

    public GestorCofres() {
        this.cofresResolvidos = new HashSet<>();
        this.listaNegraCofres = new HashSet<>();
    }

    /**
     * Gera uma chave única em texto para representar a posição do cofre (ex: "X:5,Y:12")
     */
    private String gerarChaveCoordenada(int x, int y) {
        return "X:" + x + ",Y:" + y;
    }

    /**
     * Regista que um cofre foi aberto com sucesso para o robô não perder tempo a tentar abri-lo outra vez
     */
    public void registarSucesso(int x, int y) {
        String coord = gerarChaveCoordenada(x, y);
        cofresResolvidos.add(coord);
        System.out.println("[GestorCofres] Cofre em " + coord + " resolvido com sucesso!");
    }

    /**
     * Coloca o cofre na LISTA NEGRA se a chave falhar (evita choques elétricos repetidos de -10 HP)
     */
    public void registarFalhaNaListaNegra(int x, int y) {
        String coord = gerarChaveCoordenada(x, y);
        listaNegraCofres.add(coord);
        System.err.println("[WARNING] Cofre em " + coord + " adicionado à LISTA NEGRA para proteção!");
    }

    /**
     * Verifica se o robô deve interagir com o cofre ou se é perigoso/desnecessário
     */
    public boolean podeInteragir(int x, int y) {
        String coord = gerarChaveCoordenada(x, y);

        if (cofresResolvidos.contains(coord)) {
            return false; // Já está aberto, ignorar
        }

        if (listaNegraCofres.contains(coord)) {
            System.out.println("[GestorCofres] AVISO: Movimento bloqueado para " + coord + " (Está na Lista Negra!).");
            return false; // Perigo de choque elétrico, ignorar!
        }

        return true; // Cofre novo e seguro para tentar
    }

    // Métodos para limpar a memória se mudarem de arena/reiniciarem o jogo
    public void reiniciar() {
        cofresResolvidos.clear();
        listaNegraCofres.clear();
    }
}