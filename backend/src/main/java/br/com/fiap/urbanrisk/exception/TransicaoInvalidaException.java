package br.com.fiap.urbanrisk.exception;

public class TransicaoInvalidaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
