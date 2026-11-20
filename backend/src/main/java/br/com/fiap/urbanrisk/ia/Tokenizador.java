package br.com.fiap.urbanrisk.ia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pré-processamento de texto em português para o classificador:
 * minúsculas, remoção de acentos, remoção de stopwords e stemming simples por prefixo.
 */
public final class Tokenizador {

    private static final int TAMANHO_RADICAL = 5;

    private static final Set<String> STOPWORDS = Set.copyOf(Arrays.asList(
            "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
            "em", "no", "na", "nos", "nas", "por", "pelo", "pela", "para", "pra", "com", "sem",
            "e", "ou", "que", "se", "ao", "aos", "mais", "muito", "muita", "esta", "este", "isso",
            "essa", "esse", "aqui", "ali", "la", "ja", "nao", "sim", "tem", "tinha", "foi", "ser",
            "estar", "esta", "estao", "meu", "minha", "nosso", "nossa", "rua", "avenida", "av",
            "perto", "proximo", "bairro", "hoje", "agora", "desde", "ha", "todo", "toda", "todos",
            "como", "quando", "onde", "eu", "ele", "ela", "eles", "elas", "voce", "gente"
    ));

    private Tokenizador() {}

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    public static List<String> tokenizar(String texto) {
        List<String> tokens = new ArrayList<>();
        for (String palavra : normalizar(texto).split("[^a-z0-9]+")) {
            if (palavra.length() < 3 || STOPWORDS.contains(palavra)) continue;
            if (palavra.chars().allMatch(Character::isDigit)) continue;
            tokens.add(palavra.length() > TAMANHO_RADICAL ? palavra.substring(0, TAMANHO_RADICAL) : palavra);
        }
        return tokens;
    }
}
