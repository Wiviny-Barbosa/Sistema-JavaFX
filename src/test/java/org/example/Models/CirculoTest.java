package org.example.Models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Testes do Circulo, que sobrescreve os cálculos da FormaBase. */
class CirculoTest {

    private static final double DELTA = 1e-9;

    @Test
    void areaEPerimetro() {
        Circulo c = new Circulo(new Ponto(5, 5), 2);
        assertEquals(Math.PI * 4, c.calcularArea(), DELTA);
        assertEquals(2 * Math.PI * 2, c.calcularPerimetro(), DELTA);
    }

    @Test
    void guardaSoOCentroNaListaDePontos() {
        Circulo c = new Circulo(new Ponto(5, 5), 2);
        assertEquals(1, c.getQuantidadePontos());
        assertThrows(UnsupportedOperationException.class, () -> c.adicionarPonto(new Ponto(0, 0)));
    }

    @Test
    void contemPontoPelaDistanciaAoCentro() {
        Circulo c = new Circulo(new Ponto(5, 5), 2);
        assertTrue(c.contemPonto(5, 5));
        assertTrue(c.contemPonto(7, 5)); // exatamente na borda conta como dentro
        assertFalse(c.contemPonto(7.1, 5));
    }

    @Test
    void escalarMultiplicaORaio() {
        Circulo c = new Circulo(new Ponto(0, 0), 2);
        c.escalar(1.5);
        assertEquals(3, c.getRaio(), DELTA);
    }

    @Test
    void transladarMoveOCentro() {
        Circulo c = new Circulo(new Ponto(0, 0), 2);
        c.transladar(3, 4);
        assertEquals(3, c.getCentro().getX(), DELTA);
        assertEquals(4, c.getCentro().getY(), DELTA);
    }

    @Test
    void rotacionarCirculoComumNaoMudaAForma() {
        Circulo c = new Circulo(new Ponto(1, 2), 3);
        c.rotacionar(1.2);
        assertFalse(c.isDeformado());
        assertEquals(1, c.getCentro().getX(), DELTA);
        assertEquals(2, c.getCentro().getY(), DELTA);
        assertEquals(Math.PI * 9, c.calcularArea(), DELTA);
        assertEquals(2 * Math.PI * 3, c.calcularPerimetro(), DELTA);
    }

    @Test
    void cisalharTransformaOCirculoEmElipse() {
        Circulo c = new Circulo(new Ponto(0, 0), 10);
        c.cisalhar(0.5, 0);
        assertTrue(c.isDeformado());
        // Mesmo centro e raio; o cisalhamento só muda a forma da borda
        assertEquals(0, c.getCentro().getX(), DELTA);
        assertEquals(10, c.getRaio(), DELTA);
        assertArrayEquals(new double[] {1, 0.5, 0, 1}, c.getDeformacao(), DELTA);
        // Cisalhamento puro (det = 1) mantém a área
        assertEquals(Math.PI * 100, c.calcularArea(), DELTA);
        // O topo (0, -10) vai para (-5, -10): x' = x + 0.5*y
        assertTrue(c.contemPonto(-5, -9.9));
        assertFalse(c.contemPonto(0, -10.5));
        assertFalse(c.contemPonto(5, -9.9));
    }

    @Test
    void perimetroDaElipsePelaAproximacaoDeRamanujan() {
        Circulo c = new Circulo(new Ponto(0, 0), 1);
        // Estica 3x em x: elipse de semi-eixos 3 e 1 (escala não uniforme via deformação)
        c.setDeformacao(new double[] {3, 0, 0, 1});
        double esperado = Math.PI * (3 * (3 + 1) - Math.sqrt((3 * 3 + 1) * (3 + 3 * 1)));
        assertEquals(esperado, c.calcularPerimetro(), DELTA);
        assertEquals(Math.PI * 3, c.calcularArea(), DELTA);
    }

    @Test
    void rotacionarElipseGiraOsEixos() {
        Circulo c = new Circulo(new Ponto(0, 0), 1);
        c.setDeformacao(new double[] {3, 0, 0, 1});
        c.rotacionar(Math.PI / 2);
        // Depois de 90°, o eixo maior fica na vertical
        assertTrue(c.contemPonto(0, 2.9));
        assertFalse(c.contemPonto(2.9, 0));
    }

    @Test
    void contornoSegueADeformacao() {
        Circulo c = new Circulo(new Ponto(10, 20), 2);
        c.cisalhar(1, 0);
        // t = 90°: (r*cos, r*sin) = (0, 2) -> x = 10 + 1*0 + 1*2 = 12, y = 20 + 2 = 22
        Ponto p = c.getContorno(4).get(1);
        assertEquals(12, p.getX(), 1e-9);
        assertEquals(22, p.getY(), 1e-9);
    }

    @Test
    void deformacaoQueAchataAFormaEhRecusada() {
        Circulo c = new Circulo(new Ponto(0, 0), 1);
        assertThrows(IllegalArgumentException.class, () -> c.setDeformacao(new double[] {1, 1, 1, 1}));
        // shx * shy = 1 achataria a forma: o passo é ignorado
        c.cisalhar(1, 1);
        assertFalse(c.isDeformado());
    }

    @Test
    void raioZeroOuNegativoLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new Circulo(new Ponto(0, 0), 0));
        assertThrows(IllegalArgumentException.class, () -> new Circulo(new Ponto(0, 0), -3));
        Circulo c = new Circulo(new Ponto(0, 0), 1);
        assertThrows(IllegalArgumentException.class, () -> c.setRaio(0));
    }
}
