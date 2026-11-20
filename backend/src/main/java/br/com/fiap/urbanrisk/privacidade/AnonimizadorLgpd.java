package br.com.fiap.urbanrisk.privacidade;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Proteção de dados pessoais (LGPD – Lei 13.709/2018), aplicada antes de persistir qualquer relato:
 * <ul>
 *   <li><b>Pseudonimização</b>: o identificador do dispositivo vira "USR-" + SHA-256(salt + id).
 *       O mesmo aparelho gera sempre o mesmo pseudônimo (permite correlacionar relatos),
 *       mas não é possível voltar ao identificador original.</li>
 *   <li><b>Minimização</b>: CPF, e-mail, telefone e placas digitados no texto são mascarados.</li>
 *   <li><b>Generalização de localização</b>: coordenadas de relatos comunitários são arredondadas
 *       para 3 casas decimais (~110 m), evitando expor o endereço exato do morador.</li>
 * </ul>
 */
public class AnonimizadorLgpd {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern TELEFONE = Pattern.compile("(\\+?55\\s?)?\\(?\\d{2}\\)?\\s?9?\\d{4}[-\\s]?\\d{4}\\b");
    private static final Pattern PLACA = Pattern.compile("\\b[A-Za-z]{3}-?\\d[A-Za-z0-9]\\d{2}\\b");
    public static final String MASCARA = "[dado removido - LGPD]";

    private final String salt;

    public AnonimizadorLgpd(String salt) {
        this.salt = salt == null ? "" : salt;
    }

    public String pseudonimizar(String identificadorDispositivo) {
        if (identificadorDispositivo == null || identificadorDispositivo.isBlank()) {
            return "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest((salt + identificadorDispositivo).getBytes(StandardCharsets.UTF_8));
            return "USR-" + HexFormat.of().formatHex(hash).substring(0, 8).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String mascararDadosPessoais(String texto) {
        if (texto == null) return null;
        String t = EMAIL.matcher(texto).replaceAll(MASCARA);
        t = CPF.matcher(t).replaceAll(MASCARA);
        t = TELEFONE.matcher(t).replaceAll(MASCARA);
        t = PLACA.matcher(t).replaceAll(MASCARA);
        return t;
    }

    public Double generalizarCoordenada(Double coordenada) {
        if (coordenada == null) return null;
        return Math.round(coordenada * 1000.0) / 1000.0;
    }
}
