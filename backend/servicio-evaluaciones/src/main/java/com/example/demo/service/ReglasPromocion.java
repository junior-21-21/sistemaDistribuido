package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Condiciones de promoción, recuperación pedagógica y permanencia en Secundaria (EBR) según la
 * «Norma que regula la evaluación de las competencias de los estudiantes de la Educación Básica»
 * (RVM N.° 094-2020-MINEDU, modificada por la RVM N.° 048-2024-MINEDU, numeral 5.1.3, literal A).
 *
 * <ul>
 *   <li>1.°, 3.° y 4.°: promueve con al menos «B» en la mitad o más de las competencias de cada
 *       área; permanece con «C» en más de la mitad de las competencias de cuatro o más áreas.</li>
 *   <li>2.° y 5.°: promueve con «A» o «AD» en la mitad o más de las competencias de tres áreas y
 *       «B» en las demás; permanece con «C» en la mitad o más de las competencias de cuatro o más áreas.</li>
 *   <li>En otro caso, el estudiante requiere recuperación pedagógica en las competencias con «C».</li>
 * </ul>
 */
public final class ReglasPromocion {

    public enum Situacion { PROMOVIDO, RECUPERACION, PERMANECE, SIN_NOTAS }

    /** Niveles finales de las competencias de un área (por ejemplo, Matemática: [A, B, C, C]). */
    public record Area(Long cursoId, List<String> niveles) {
        long contar(Set<String> buscados) {
            return niveles.stream().filter(buscados::contains).count();
        }
        int total() {
            return niveles.size();
        }
    }

    public record Resultado(Situacion situacion, String motivo, List<Long> areasConC) {
    }

    private static final Set<String> B_O_MAS = Set.of("AD", "A", "B");
    private static final Set<String> A_O_AD = Set.of("AD", "A");
    private static final Set<String> C = Set.of("C");

    private ReglasPromocion() {
    }

    /** Grados 2.° y 5.° tienen exigencia mayor (fin de ciclo VI y VII). */
    public static boolean esFinDeCiclo(int grado) {
        return grado == 2 || grado == 5;
    }

    public static Resultado alTerminoDelAnio(int grado, List<Area> areas) {
        List<Area> conNotas = areas.stream().filter(a -> a.total() > 0).toList();
        if (conNotas.isEmpty()) {
            return new Resultado(Situacion.SIN_NOTAS, "Aún no tiene calificaciones registradas", List.of());
        }
        List<Long> areasConC = conNotas.stream().filter(a -> a.contar(C) > 0).map(Area::cursoId).toList();

        if (esFinDeCiclo(grado)) {
            long areasConLogroAlto = conNotas.stream().filter(a -> 2 * a.contar(A_O_AD) >= a.total()).count();
            boolean sinC = areasConC.isEmpty();
            if (areasConLogroAlto >= Math.min(3, conNotas.size()) && sinC) {
                return new Resultado(Situacion.PROMOVIDO,
                        "Logro A o AD en la mitad o más de las competencias de " + areasConLogroAlto + " áreas y B en las demás",
                        List.of());
            }
            long areasEnRiesgo = conNotas.stream().filter(a -> 2 * a.contar(C) >= a.total()).count();
            if (areasEnRiesgo >= 4) {
                return new Resultado(Situacion.PERMANECE,
                        "C en la mitad o más de las competencias de " + areasEnRiesgo + " áreas", areasConC);
            }
        } else {
            boolean todasConB = conNotas.stream().allMatch(a -> 2 * a.contar(B_O_MAS) >= a.total());
            if (todasConB) {
                return new Resultado(Situacion.PROMOVIDO,
                        "Al menos B en la mitad o más de las competencias de cada área", areasConC);
            }
            long areasEnRiesgo = conNotas.stream().filter(a -> 2 * a.contar(C) > a.total()).count();
            if (areasEnRiesgo >= 4) {
                return new Resultado(Situacion.PERMANECE,
                        "C en más de la mitad de las competencias de " + areasEnRiesgo + " áreas", areasConC);
            }
        }
        return new Resultado(Situacion.RECUPERACION,
                "Debe recuperar las competencias con C en " + areasConC.size() + " área(s)", areasConC);
    }

    /** Nivel final de una competencia: el del último bimestre calificado. */
    public static String nivelFinal(Map<Integer, String> nivelPorBimestre) {
        return nivelPorBimestre.entrySet().stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).orElse(null);
    }

    /** Convierte «I BIMESTRE», «IV BIMESTRE», «BIMESTRE_2», etc. en su número (1 a 4). */
    public static int numeroBimestre(String bimestre) {
        if (bimestre == null) return 0;
        String b = bimestre.trim().toUpperCase();
        if (b.startsWith("IV")) return 4;
        if (b.startsWith("III")) return 3;
        if (b.startsWith("II")) return 2;
        if (b.startsWith("I ") || b.equals("I")) return 1;
        String digitos = b.replaceAll("\\D", "");
        return digitos.isEmpty() ? 0 : Integer.parseInt(digitos.substring(0, 1));
    }
}
