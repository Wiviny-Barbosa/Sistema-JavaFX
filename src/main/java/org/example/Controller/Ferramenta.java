package org.example.Controller;

import javafx.scene.input.MouseEvent;

/**
 * Padrão State: cada ferramenta do editor é um "estado" diferente.
 *
 * O EditorContext guarda qual ferramenta está ativa e simplesmente repassa
 * os eventos do mouse para ela. Assim, o mesmo clique no canvas desenha,
 * seleciona ou apaga dependendo da ferramenta, sem nenhum if/switch por tipo.
 * Para criar uma ferramenta nova, basta uma classe nova que implemente esta interface.
 */
public interface Ferramenta {

    void aoPressionarMouse(MouseEvent evento, EditorContext contexto);

    void aoArrastarMouse(MouseEvent evento, EditorContext contexto);

    void aoSoltarMouse(MouseEvent evento, EditorContext contexto);

    void aoClicar(MouseEvent evento, EditorContext contexto);

    /**
     * Tecla Enter: confirma o que a ferramenta estiver fazendo (a Linha Livre
     * fecha o polígono). Por padrão não faz nada, então só quem usa sobrescreve.
     */
    default void aoConfirmar(EditorContext contexto) {
    }
}
