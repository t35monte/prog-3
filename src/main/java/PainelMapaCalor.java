import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class PainelMapaCalor extends JPanel {

    private static final int TAMANHO_CELULA = 20;
    private static final int LARGURA_GRELHA = 30;
    private static final int ALTURA_GRELHA = 30;

    private Map<String, Integer> historicoVisitas;
    private int robotX = 0;
    private int robotY = 0;
    private int robotHp = 100;
    private String ultimaAcao = "-";

    private static JFrame janela;

    public PainelMapaCalor() {
        setPreferredSize(new Dimension(
                LARGURA_GRELHA * TAMANHO_CELULA,
                ALTURA_GRELHA * TAMANHO_CELULA + 80
        ));
        setBackground(Color.BLACK);
    }

    public void atualizar(Map<String, Integer> historicoVisitas, int x, int y, int hp, String acao) {
        this.historicoVisitas = historicoVisitas;
        this.robotX = x;
        this.robotY = y;
        this.robotHp = hp;
        this.ultimaAcao = acao;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (historicoVisitas == null) return;

        int maxVisitas = historicoVisitas.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        if (maxVisitas == 0) maxVisitas = 1; // evitar divisão por zero

        for (int col = 0; col < LARGURA_GRELHA; col++) {
            for (int linha = 0; linha < ALTURA_GRELHA; linha++) {
                String coord = col + "," + linha;
                int visitas = historicoVisitas.getOrDefault(coord, 0);

                // CORREÇÃO: garantir que intensidade está entre 0.0 e 1.0
                float intensidade = Math.max(0f, Math.min(1f, (float) visitas / maxVisitas));
                int vermelho = (int) (intensidade * 255);
                int azul = (int) ((1 - intensidade) * 255);
                // Garantir que os valores estão no intervalo [0, 255]
                vermelho = Math.max(0, Math.min(255, vermelho));
                azul = Math.max(0, Math.min(255, azul));

                g2d.setColor(new Color(vermelho, 0, azul));
                g2d.fillRect(col * TAMANHO_CELULA, linha * TAMANHO_CELULA, TAMANHO_CELULA - 1, TAMANHO_CELULA - 1);
            }
        }

        // Desenhar o robô
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(
                robotX * TAMANHO_CELULA + 2,
                robotY * TAMANHO_CELULA + 2,
                TAMANHO_CELULA - 4,
                TAMANHO_CELULA - 4
        );

        // Telemetria em baixo
        int baseY = ALTURA_GRELHA * TAMANHO_CELULA + 20;
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2d.drawString("Posição: (" + robotX + ", " + robotY + ")", 10, baseY);
        g2d.drawString("HP: " + robotHp, 200, baseY);
        g2d.drawString("Última ação: " + ultimaAcao, 300, baseY);

        // Barra de HP
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(10, baseY + 10, 150, 15);
        int hpBarWidth = (int) (150 * Math.max(0, Math.min(robotHp, 200)) / 200.0);
        g2d.setColor(robotHp > 100 ? Color.GREEN : Color.RED);
        g2d.fillRect(10, baseY + 10, hpBarWidth, 15);
        g2d.setColor(Color.WHITE);
        g2d.drawString("HP: " + robotHp, 170, baseY + 23);
    }

    public static PainelMapaCalor criar() {
        janela = new JFrame("RoboXPTO — Mapa de Calor");
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PainelMapaCalor painel = new PainelMapaCalor();
        janela.add(painel);
        janela.pack();
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);
        return painel;
    }
}