public class DocumentoVetorial {
    private String texto;
    private double[] vetor;

    // Construtor
    public DocumentoVetorial(String texto, double[] vetor) {
        this.texto = texto;
        this.vetor = vetor;
    }

    // Getters e Setters
    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public double[] getVetor() {
        return vetor;
    }

    public void setVetor(double[] vetor) {
        this.vetor = vetor;
    }
}