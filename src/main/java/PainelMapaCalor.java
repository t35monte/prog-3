import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class PainelMapaCalor extends JPanel {

    // Tamanho de cada célula da grelha em pixels
    private static final int TAMANHO_CELULA = 20;

    // Tamanho da grelha (arena)
    private static final int LARGURA_GRELHA = 30;
    private static final int ALTURA_GRELHA = 30;

    // Dados do robô atualizados a cada turno
    private Map<String, Integer> historicoVisitas;
    private int robotX = 0;
    private int robotY = 0;
    private int robotHp = 100;
    private String ultimaAcao = "-";

    // Janela principal
    private static JFrame janela;

    public PainelMapaCalor() {
        // Tamanho do painel = grelha + espaço para telemetria em baixo
        setPreferredSize(new Dimension(
                LARGURA_GRELHA * TAMANHO_CELULA,
                ALTURA_GRELHA * TAMANHO_CELULA + 80
        ));
        setBackground(Color.BLACK);
    }

    // Chamado pelo AgenteExplorador a cada turno para atualizar os dados
    public void atualizar(Map<String, Integer> historicoVisitas, int x, int y, int hp, String acao) {
        this.historicoVisitas = historicoVisitas;
        this.robotX = x;
        this.robotY = y;
        this.robotHp = hp;
        this.ultimaAcao = acao;
        repaint(); // Redesenhar o painel
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (historicoVisitas == null) return;

        // Encontrar o valor máximo de visitas para normalizar as cores
        int maxVisitas = historicoVisitas.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);

        // === DESENHAR MAPA DE CALOR ===
        for (int col = 0; col < LARGURA_GRELHA; col++) {
            for (int linha = 0; linha < ALTURA_GRELHA; linha++) {
                String coord = col + "," + linha;
                int visitas = historicoVisitas.getOrDefault(coord, 0);

                // Calcular cor: azul frio (pouco visitado) → vermelho quente (muito visitado)
                float intensidade = (float) visitas / maxVisitas;
                Color cor = new Color(
                        (int) (intensidade * 255), // vermelho aumenta com visitas
                        0,
                        (int) ((1 - intensidade) * 255) // azul diminui com visitas
                );

                g2d.setColor(cor);
                g2d.fillRect(
                        col * TAMANHO_CELULA,
                        linha * TAMANHO_CELULA,
                        TAMANHO_CELULA - 1,
                        TAMANHO_CELULA - 1
                );
            }
        }

        // === DESENHAR O ROBÔ ===
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(
                robotX * TAMANHO_CELULA + 2,
                robotY * TAMANHO_CELULA + 2,
                TAMANHO_CELULA - 4,
                TAMANHO_CELULA - 4
        );

        // === DESENHAR TELEMETRIA EM BAIXO ===
        int baseY = ALTURA_GRELHA * TAMANHO_CELULA + 20;

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2d.drawString("Posição: (" + robotX + ", " + robotY + ")", 10, baseY);
        g2d.drawString("HP: " + robotHp, 200, baseY);
        g2d.drawString("Última ação: " + ultimaAcao, 300, baseY);

        // Barra de HP
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(10, baseY + 10, 150, 15);
        g2d.setColor(robotHp > 50 ? Color.GREEN : Color.RED);
        g2d.fillRect(10, baseY + 10, (int) (150 * robotHp / 100.0), 15);
        g2d.setColor(Color.WHITE);
        g2d.drawString("HP: " + robotHp + "%", 170, baseY + 23);
    }

    // Método estático para criar e mostrar a janela
    public static PainelMapaCalor criar() {
        janela = new JFrame("RoboXPTO — Mapa de Calor");
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        PainelMapaCalor painel = new PainelMapaCalor();
        janela.add(painel);
        janela.pack();
        janela.setLocationRelativeTo(null); // Centrar no ecrã
        janela.setVisible(true);

        return painel;
    }
}