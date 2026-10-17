package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.demo.service.ReglasPromocion.Area;
import com.example.demo.service.ReglasPromocion.Situacion;

/** Casos de la tabla de la RVM 094-2020-MINEDU modificada por la RVM 048-2024-MINEDU (Secundaria). */
class ReglasPromocionTest {

    private static Area area(long id, String... niveles) {
        return new Area(id, List.of(niveles));
    }

    @Test
    void primerGradoPromueveConBEnLaMitadDeCadaArea() {
        var r = ReglasPromocion.alTerminoDelAnio(1, List.of(
                area(1, "B", "C"), area(2, "A", "A", "C"), area(3, "AD")));
        assertThat(r.situacion()).isEqualTo(Situacion.PROMOVIDO);
    }

    @Test
    void primerGradoVaARecuperacionSiUnAreaTieneMenosDeLaMitadEnB() {
        var r = ReglasPromocion.alTerminoDelAnio(3, List.of(
                area(1, "C", "C", "B"), area(2, "A", "A"), area(3, "B", "B")));
        assertThat(r.situacion()).isEqualTo(Situacion.RECUPERACION);
        assertThat(r.areasConC()).containsExactly(1L);
    }

    @Test
    void primerGradoPermaneceConCEnMasDeLaMitadDeCuatroAreas() {
        var r = ReglasPromocion.alTerminoDelAnio(4, List.of(
                area(1, "C", "C", "B"), area(2, "C", "C"), area(3, "C", "C", "C"), area(4, "C", "B", "C"), area(5, "A")));
        assertThat(r.situacion()).isEqualTo(Situacion.PERMANECE);
    }

    @Test
    void segundoGradoExigeAoADEnTresAreasYBEnLasDemas() {
        var promovido = ReglasPromocion.alTerminoDelAnio(2, List.of(
                area(1, "A", "B"), area(2, "AD", "A"), area(3, "A", "B"), area(4, "B", "B")));
        assertThat(promovido.situacion()).isEqualTo(Situacion.PROMOVIDO);

        // Solo dos áreas con A/AD: no cumple la promoción, tampoco la permanencia → recuperación
        var recuperacion = ReglasPromocion.alTerminoDelAnio(5, List.of(
                area(1, "A", "B"), area(2, "AD", "A"), area(3, "B", "B"), area(4, "B", "C")));
        assertThat(recuperacion.situacion()).isEqualTo(Situacion.RECUPERACION);
    }

    @Test
    void quintoGradoPermaneceConCEnLaMitadDeCuatroAreas() {
        var r = ReglasPromocion.alTerminoDelAnio(5, List.of(
                area(1, "C", "B"), area(2, "C", "A"), area(3, "C", "C"), area(4, "B", "C"), area(5, "AD")));
        assertThat(r.situacion()).isEqualTo(Situacion.PERMANECE);
    }

    @Test
    void sinNotasNoSugiereNada() {
        assertThat(ReglasPromocion.alTerminoDelAnio(1, List.of()).situacion()).isEqualTo(Situacion.SIN_NOTAS);
    }

    @Test
    void elNivelFinalEsElDelUltimoBimestreCalificado() {
        assertThat(ReglasPromocion.nivelFinal(Map.of(1, "C", 2, "B", 4, "A"))).isEqualTo("A");
        assertThat(ReglasPromocion.numeroBimestre("IV BIMESTRE")).isEqualTo(4);
        assertThat(ReglasPromocion.numeroBimestre("II BIMESTRE")).isEqualTo(2);
        assertThat(ReglasPromocion.numeroBimestre("I BIMESTRE")).isEqualTo(1);
        assertThat(ReglasPromocion.numeroBimestre("BIMESTRE_3")).isEqualTo(3);
    }
}
