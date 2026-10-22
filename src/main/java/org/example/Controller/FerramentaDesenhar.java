package org.example.Controller;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.example.Models.FormaGeometrica;
import org.example.Models.Poligono;
import org.example.Models.Ponto;

/**
 * Ferramenta "Linha Livre": desenha um polígono ponto a ponto.
 * - Arrastar o mouse com o botão esquerdo desenha a linha seguindo o mouse.
 * - Cada clique (sem arrastar) adiciona um vértice.
 * Enquanto não é fechada, a forma aparece como uma linha aberta. O polígono é
 * fechado e preenchido com Enter, duplo clique, clique direito ou clicando
 * perto do primeiro ponto.
 */
public class FerramentaDesenhar implements Ferramenta {

    // Clicar a menos disso do primeiro ponto fecha o polígono
    private static final double DISTANCIA_PARA_FECHAR = 10;

    // No arrasto, só entra um ponto novo a cada 4 px, para a linha não ter milhares de pontos
    private static final double DISTANCIA_MINIMA_ARRASTO = 4;

    // Onde o botão foi pressionado (null se não há botão esquerdo pressionado)
    private Ponto pontoPressionado;

    // Fica true a partir do primeiro movimento depois de pressionar
    private boolean arrastando;

    @Override
    public void aoPressionarMouse(MouseEvent evento, EditorContext contexto) {
        if (evento.getButton() != MouseButton.PRIMARY) {
            return;
        }
        pontoPressionado = new Ponto(contexto.aplicarSnap(evento.getX()), contexto.aplicarSnap(evento.getY()));
        arrastando = false;
    }

    @Override
    public void aoArrastarMouse(MouseEvent evento, EditorContext contexto) {
        if (pontoPressionado == null) {
            return;
        }
        if (!arrastando) {
            // Primeiro movimento: a linha começa onde o botão foi pressionado
            arrastando = true;
            adicionarSeLonge(pontoPressionado, contexto);
        }
        adicionarSeLonge(new Ponto(contexto.aplicarSnap(evento.getX()), contexto.aplicarSnap(evento.getY())), contexto);
        contexto.requestRedraw();
    }

    @Override
    public void aoSoltarMouse(MouseEvent evento, EditorContext contexto) {
        // A linha continua em andamento: o usuário pode arrastar de novo, clicar ou fechar com Enter
        pontoPressionado = null;
        arrastando = false;
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoClicar(MouseEvent evento, EditorContext contexto) {
        if (evento.getButton() == MouseButton.SECONDARY || evento.getClickCount() == 2) {
            finalizarPoligono(contexto);
            return;
        }
        // O JavaFX também gera um "clique" ao soltar depois de arrastar; os pontos já entraram no arrasto
        if (evento.getButton() != MouseButton.PRIMARY || !evento.isStillSincePress()) {
            return;
        }

        Ponto novo = new Ponto(contexto.aplicarSnap(evento.getX()), contexto.aplicarSnap(evento.getY()));
        FormaGeometrica emAndamento = contexto.getFormaEmAndamento();

        if (emAndamento != null
                && emAndamento.getQuantidadePontos() >= Poligono.MINIMO_PONTOS
                && novo.distancia(emAndamento.getPontos().get(0)) <= DISTANCIA_PARA_FECHAR) {
            finalizarPoligono(contexto);
            return;
        }

        obterOuCriarEmAndamento(contexto).adicionarPonto(novo);
        contexto.requestRedraw();
    }

    /** Enter fecha e preenche a linha que está sendo desenhada. */
    @Override
    public void aoConfirmar(EditorContext contexto) {
        finalizarPoligono(contexto);
    }

    /** Adiciona o ponto do arrasto, a menos que ele esteja colado no último ponto da linha. */
    private void adicionarSeLonge(Ponto ponto, EditorContext contexto) {
        FormaGeometrica emAndamento = obterOuCriarEmAndamento(contexto);
        int n = emAndamento.getQuantidadePontos();
        if (n > 0 && ponto.distancia(emAndamento.getPontos().get(n - 1)) < DISTANCIA_MINIMA_ARRASTO) {
            return;
        }
        emAndamento.adicionarPonto(ponto);
    }

    /** Devolve a linha em andamento ou começa uma nova com o estilo da barra lateral. */
    private FormaGeometrica obterOuCriarEmAndamento(EditorContext contexto) {
        FormaGeometrica emAndamento = contexto.getFormaEmAndamento();
        if (emAndamento == null) {
            emAndamento = new Poligono();
            contexto.aplicarEstiloAtual(emAndamento);
            contexto.setFormaEmAndamento(emAndamento);
        }
        return emAndamento;
    }

    /**
     * Transforma os pontos desenhados num Poligono de verdade. O construtor do
     * Poligono confere a regra dos 3 pontos; se faltar ponto, ele lança
     * IllegalArgumentException e o usuário pode continuar desenhando.
     */
    private void finalizarPoligono(EditorContext contexto) {
        FormaGeometrica emAndamento = contexto.getFormaEmAndamento();
        if (emAndamento == null) {
            return;
        }
        try {
            Poligono poligono = new Poligono(emAndamento.getPontos());
            contexto.aplicarEstiloAtual(poligono);
            contexto.registrarEstado();
            contexto.adicionarForma(poligono);
            contexto.setFormaEmAndamento(null);
        } catch (IllegalArgumentException e) {
            contexto.mostrarAviso(e.getMessage() + " Continue desenhando para adicionar pontos.");
        }
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }
}
