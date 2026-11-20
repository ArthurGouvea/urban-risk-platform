package br.com.fiap.urbanrisk.ia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classificador Naive Bayes multinomial com suavização de Laplace, implementado do zero.
 * É treinado na inicialização do backend a partir de um corpus rotulado de relatos urbanos
 * (src/main/resources/ia/corpus-relatos.csv).
 */
public class NaiveBayesMultinomial {

    /** Exemplo rotulado de treino. */
    public record Exemplo(String texto, String rotulo) {}

    private final Map<String, Integer> documentosPorClasse = new HashMap<>();
    private final Map<String, Map<String, Integer>> contagemTokens = new HashMap<>();
    private final Map<String, Integer> totalTokensPorClasse = new HashMap<>();
    private final Set<String> vocabulario = new HashSet<>();
    private int totalDocumentos;

    public void treinar(List<Exemplo> exemplos) {
        for (Exemplo e : exemplos) {
            documentosPorClasse.merge(e.rotulo(), 1, Integer::sum);
            totalDocumentos++;
            Map<String, Integer> tokensClasse = contagemTokens.computeIfAbsent(e.rotulo(), k -> new HashMap<>());
            for (String token : Tokenizador.tokenizar(e.texto())) {
                tokensClasse.merge(token, 1, Integer::sum);
                totalTokensPorClasse.merge(e.rotulo(), 1, Integer::sum);
                vocabulario.add(token);
            }
        }
    }

    /**
     * Retorna a probabilidade a posteriori de cada classe (soma = 1), ordenada da maior para a menor.
     */
    public Map<String, Double> predizer(String texto) {
        if (totalDocumentos == 0) throw new IllegalStateException("Modelo não treinado");
        List<String> tokens = Tokenizador.tokenizar(texto);
        Map<String, Double> logProb = new HashMap<>();
        int v = vocabulario.size();

        for (String classe : documentosPorClasse.keySet()) {
            double lp = Math.log((double) documentosPorClasse.get(classe) / totalDocumentos);
            Map<String, Integer> tokensClasse = contagemTokens.getOrDefault(classe, Map.of());
            int total = totalTokensPorClasse.getOrDefault(classe, 0);
            for (String token : tokens) {
                if (!vocabulario.contains(token)) continue; // palavra desconhecida não informa nada
                lp += Math.log((tokensClasse.getOrDefault(token, 0) + 1.0) / (total + v));
            }
            logProb.put(classe, lp);
        }

        // softmax numericamente estável
        double max = Collections.max(logProb.values());
        double soma = 0;
        Map<String, Double> prob = new HashMap<>();
        for (var entry : logProb.entrySet()) {
            double p = Math.exp(entry.getValue() - max);
            prob.put(entry.getKey(), p);
            soma += p;
        }
        final double total = soma;
        Map<String, Double> ordenado = new LinkedHashMap<>();
        prob.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(en -> ordenado.put(en.getKey(), en.getValue() / total));
        return ordenado;
    }

    public String classeMaisProvavel(String texto) {
        return predizer(texto).keySet().iterator().next();
    }

    /** Conta quantas palavras do texto o modelo conhece (0 = o modelo não tem evidência). */
    public int tokensConhecidos(String texto) {
        int n = 0;
        for (String t : Tokenizador.tokenizar(texto)) if (vocabulario.contains(t)) n++;
        return n;
    }

    public int tamanhoVocabulario() { return vocabulario.size(); }

    public int totalExemplos() { return totalDocumentos; }

    /**
     * Validação cruzada leave-one-out: treina com N-1 exemplos e testa no exemplo restante.
     * Retorna a acurácia (0..1). Usada para reportar a qualidade do modelo na API.
     */
    public static double acuraciaLeaveOneOut(List<Exemplo> exemplos) {
        if (exemplos.size() < 2) return 0;
        int acertos = 0;
        for (int i = 0; i < exemplos.size(); i++) {
            List<Exemplo> treino = new ArrayList<>(exemplos);
            Exemplo teste = treino.remove(i);
            NaiveBayesMultinomial modelo = new NaiveBayesMultinomial();
            modelo.treinar(treino);
            if (modelo.classeMaisProvavel(teste.texto()).equals(teste.rotulo())) acertos++;
        }
        return (double) acertos / exemplos.size();
    }
}
