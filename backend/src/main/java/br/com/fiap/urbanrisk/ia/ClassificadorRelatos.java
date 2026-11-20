package br.com.fiap.urbanrisk.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Componente de NLP da plataforma. Treina dois modelos Naive Bayes a partir do corpus rotulado:
 * <ul>
 *   <li><b>categoria</b>: ALAGAMENTO, ILHA_DE_CALOR, FALTA_ENERGIA ou FALTA_AGUA;</li>
 *   <li><b>urgência</b>: ALTA ou BAIXA (risco à vida, pessoas vulneráveis, evolução rápida...).</li>
 * </ul>
 * O resultado é usado para (1) sugerir/corrigir a categoria de um relato comunitário,
 * (2) medir a coerência entre a categoria escolhida e o texto (afeta a confiança) e
 * (3) estimar a urgência (afeta a prioridade).
 */
public class ClassificadorRelatos {

    /** Resultado da análise de um texto. */
    public record AnaliseTexto(
            String categoriaSugerida,
            double probabilidadeCategoriaSugerida,
            Map<String, Double> probabilidadesCategoria,
            double probabilidadeUrgencia,
            double coerenciaCategoriaInformada,
            int palavrasReconhecidas
    ) {
        public boolean temEvidencia() { return palavrasReconhecidas > 0; }
    }

    private final NaiveBayesMultinomial modeloCategoria = new NaiveBayesMultinomial();
    private final NaiveBayesMultinomial modeloUrgencia = new NaiveBayesMultinomial();
    private final double acuraciaCategoria;
    private final double acuraciaUrgencia;
    private final int totalExemplos;

    public ClassificadorRelatos(List<String[]> linhasCorpus) {
        List<NaiveBayesMultinomial.Exemplo> exCategoria = new ArrayList<>();
        List<NaiveBayesMultinomial.Exemplo> exUrgencia = new ArrayList<>();
        for (String[] l : linhasCorpus) {
            exCategoria.add(new NaiveBayesMultinomial.Exemplo(l[0], l[1]));
            exUrgencia.add(new NaiveBayesMultinomial.Exemplo(l[0], l[2]));
        }
        modeloCategoria.treinar(exCategoria);
        modeloUrgencia.treinar(exUrgencia);
        acuraciaCategoria = NaiveBayesMultinomial.acuraciaLeaveOneOut(exCategoria);
        acuraciaUrgencia = NaiveBayesMultinomial.acuraciaLeaveOneOut(exUrgencia);
        totalExemplos = linhasCorpus.size();
    }

    /** Lê o corpus no formato "texto;categoria;urgencia" (com cabeçalho). */
    public static ClassificadorRelatos deCsv(InputStream csv) throws IOException {
        List<String[]> linhas = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String linha = r.readLine(); // cabeçalho
            while ((linha = r.readLine()) != null) {
                if (linha.isBlank()) continue;
                String[] partes = linha.split(";");
                if (partes.length == 3) linhas.add(new String[]{partes[0].trim(), partes[1].trim(), partes[2].trim()});
            }
        }
        return new ClassificadorRelatos(linhas);
    }

    public AnaliseTexto analisar(String texto, String categoriaInformada) {
        Map<String, Double> probs = modeloCategoria.predizer(texto);
        var melhor = probs.entrySet().iterator().next();
        int reconhecidas = modeloCategoria.tokensConhecidos(texto);

        // Naive Bayes tende a probabilidades extremas; calibramos para a faixa 10%–90%
        double urgencia = reconhecidas == 0 ? 0.5 : 0.1 + 0.8 * modeloUrgencia.predizer(texto).getOrDefault("ALTA", 0.5);
        double coerencia = categoriaInformada == null
                ? melhor.getValue()
                : probs.getOrDefault(categoriaInformada.toUpperCase(), 0.0);
        if (reconhecidas == 0) coerencia = 0.5; // sem evidência: neutro

        return new AnaliseTexto(melhor.getKey(), melhor.getValue(), probs, urgencia, coerencia, reconhecidas);
    }

    public double getAcuraciaCategoria() { return acuraciaCategoria; }
    public double getAcuraciaUrgencia() { return acuraciaUrgencia; }
    public int getTotalExemplos() { return totalExemplos; }
    public int getTamanhoVocabulario() { return modeloCategoria.tamanhoVocabulario(); }
}
