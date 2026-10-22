package org.example.Models;

import java.util.ArrayList;
import java.util.List;

/**
 * Círculo: guarda o centro como único ponto da lista e o raio num atributo próprio.
 * Como a matemática de polígono da FormaBase não serve para ele, os cálculos e
 * as transformações são sobrescritos aqui.
 *
 * Para poder ser cisalhado, o círculo guarda também uma matriz 2x2 de deformação
 * (m00 m01 / m10 m11). Cada ponto da borda é: centro + M * (raio*cos t, raio*sin t).
 * Sem deformação a matriz é a identidade e a forma é um círculo comum; depois de
 * cisalhada ela vira uma elipse, que pode continuar sendo girada e escalada.
 */
public class Circulo extends FormaBase {

    // Abaixo disso a matriz "achata" a forma numa linha e não pode ser invertida
    private static final double DETERMINANTE_MINIMO = 1e-6;

    private double raio;

    // Matriz de deformação; começa como identidade (círculo sem deformação)
    private double m00 = 1;
    private double m01 = 0;
    private double m10 = 0;
    private double m11 = 1;

    public Circulo(Ponto centro, double raio) {
        super();
        if (centro == null) {
            throw new IllegalArgumentException("O centro não pode ser nulo.");
        }
        validarRaio(raio);
        pontos.add(centro);
        this.raio = raio;
    }

    // Usado só pelo clonar()
    private Circulo() {
        super();
    }

    public Ponto getCentro() {
        return pontos.get(0);
    }

    public double getRaio() {
        return raio;
    }

    public void setRaio(double raio) {
        validarRaio(raio);
        this.raio = raio;
    }

    private void validarRaio(double raio) {
        if (raio <= 0) {
            throw new IllegalArgumentException("O raio do círculo deve ser maior que zero.");
        }
    }

    /** Matriz de deformação como {m00, m01, m10, m11} (cópia: alterar o array não muda o círculo). */
    public double[] getDeformacao() {
        return new double[] {m00, m01, m10, m11};
    }

    /** Define a matriz de deformação {m00, m01, m10, m11} (usado ao carregar um arquivo). */
    public void setDeformacao(double[] matriz) {
        if (matriz == null || matriz.length != 4) {
            throw new IllegalArgumentException("A deformação do círculo precisa de 4 valores.");
        }
        for (double valor : matriz) {
            if (!Double.isFinite(valor)) {
                throw new IllegalArgumentException("A deformação do círculo tem um valor inválido.");
            }
        }
        if (Math.abs(matriz[0] * matriz[3] - matriz[1] * matriz[2]) < DETERMINANTE_MINIMO) {
            throw new IllegalArgumentException("A deformação do círculo achata a forma numa linha.");
        }
        m00 = matriz[0];
        m01 = matriz[1];
        m10 = matriz[2];
        m11 = matriz[3];
    }

    /** true se o círculo foi cisalhado (virou elipse). */
    public boolean isDeformado() {
        // M * M^T = identidade quando a matriz só gira (círculo continua círculo)
        double a = m00 * m00 + m01 * m01;
        double b = m00 * m10 + m01 * m11;
        double d = m10 * m10 + m11 * m11;
        return Math.abs(a - 1) > 1e-9 || Math.abs(b) > 1e-9 || Math.abs(d - 1) > 1e-9;
    }

    /**
     * Pontos ao longo da borda, para desenhar a forma (deformada ou não) como polígono:
     * x = cx + m00*r*cos(t) + m01*r*sin(t),  y = cy + m10*r*cos(t) + m11*r*sin(t)
     */
    public List<Ponto> getContorno(int quantidade) {
        if (quantidade < 3) {
            throw new IllegalArgumentException("O contorno precisa de pelo menos 3 pontos.");
        }
        Ponto c = getCentro();
        List<Ponto> contorno = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            double t = 2 * Math.PI * i / quantidade;
            double ux = raio * Math.cos(t);
            double uy = raio * Math.sin(t);
            contorno.add(new Ponto(c.getX() + m00 * ux + m01 * uy, c.getY() + m10 * ux + m11 * uy));
        }
        return contorno;
    }

    /** O círculo é definido só pelo centro, então não aceita pontos extras. */
    @Override
    public void adicionarPonto(Ponto p) {
        throw new UnsupportedOperationException("O círculo tem apenas o ponto central.");
    }

    /** pi * r^2 vezes |det(M)|, que é quanto a matriz aumenta ou diminui a área. */
    @Override
    public double calcularArea() {
        return Math.PI * raio * raio * Math.abs(determinante());
    }

    /**
     * Sem deformação: 2 * pi * r. Com deformação é uma elipse de semi-eixos a e b,
     * e o perímetro usa a aproximação de Ramanujan:
     * pi * (3(a + b) - raiz((3a + b)(a + 3b)))
     */
    @Override
    public double calcularPerimetro() {
        double[] semiEixos = semiEixos();
        double a = semiEixos[0];
        double b = semiEixos[1];
        return Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)));
    }

    /**
     * Semi-eixos da elipse = raio * valores singulares da matriz M.
     * Fórmula fechada para matriz 2x2:
     * E = (m00+m11)/2, F = (m00-m11)/2, G = (m10+m01)/2, H = (m10-m01)/2,
     * Q = raiz(E^2+H^2), R = raiz(F^2+G^2), valores singulares = Q+R e |Q-R|.
     */
    private double[] semiEixos() {
        double e = (m00 + m11) / 2;
        double f = (m00 - m11) / 2;
        double g = (m10 + m01) / 2;
        double h = (m10 - m01) / 2;
        double q = Math.sqrt(e * e + h * h);
        double r = Math.sqrt(f * f + g * g);
        return new double[] {raio * (q + r), raio * Math.abs(q - r)};
    }

    private double determinante() {
        return m00 * m11 - m01 * m10;
    }

    @Override
    public Ponto getCentroide() {
        return getCentro().clonar();
    }

    /**
     * Desfaz a deformação no ponto (multiplica pela inversa de M) e confere se ele
     * cai dentro do círculo original: distância ao centro <= raio.
     */
    @Override
    public boolean contemPonto(double px, double py) {
        Ponto c = getCentro();
        double vx = px - c.getX();
        double vy = py - c.getY();
        double det = determinante();
        double ux = (m11 * vx - m01 * vy) / det;
        double uy = (-m10 * vx + m00 * vy) / det;
        return Math.sqrt(ux * ux + uy * uy) <= raio;
    }

    // transladar() não precisa ser sobrescrito: mover o centro já move o círculo

    @Override
    public void escalar(double fator) {
        validarFatorEscala(fator);
        raio *= fator;
    }

    /**
     * Gira a deformação: M = Rot(ângulo) * M. Num círculo comum isso não muda o
     * desenho, mas numa elipse (círculo cisalhado) gira os eixos dela.
     */
    @Override
    public void rotacionar(double anguloRadianos) {
        double cos = Math.cos(anguloRadianos);
        double sin = Math.sin(anguloRadianos);
        aplicarNaDeformacao(cos, -sin, sin, cos);
    }

    /**
     * Cisalhamento relativo ao centro (x' = x + shx*y, y' = y + shy*x):
     * M = [[1, shx], [shy, 1]] * M. O círculo vira uma elipse.
     */
    @Override
    public void cisalhar(double shx, double shy) {
        aplicarNaDeformacao(1, shx, shy, 1);
    }

    /**
     * Multiplica a transformação T = [[t00, t01], [t10, t11]] à esquerda de M.
     * Se o resultado achatar a forma numa linha, o passo é ignorado.
     */
    private void aplicarNaDeformacao(double t00, double t01, double t10, double t11) {
        double n00 = t00 * m00 + t01 * m10;
        double n01 = t00 * m01 + t01 * m11;
        double n10 = t10 * m00 + t11 * m10;
        double n11 = t10 * m01 + t11 * m11;
        if (Math.abs(n00 * n11 - n01 * n10) < DETERMINANTE_MINIMO) {
            return;
        }
        m00 = n00;
        m01 = n01;
        m10 = n10;
        m11 = n11;
    }

    @Override
    public FormaGeometrica clonar() {
        Circulo copia = new Circulo();
        copiarAtributosPara(copia);
        copia.raio = this.raio;
        copia.m00 = this.m00;
        copia.m01 = this.m01;
        copia.m10 = this.m10;
        copia.m11 = this.m11;
        return copia;
    }
}
