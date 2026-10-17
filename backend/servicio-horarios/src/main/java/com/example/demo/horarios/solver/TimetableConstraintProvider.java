package com.example.demo.horarios.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.example.demo.horarios.domain.Lesson;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints (Restricciones Duras)
                roomConflict(constraintFactory),
                teacherConflict(constraintFactory),
                studentGroupConflict(constraintFactory),
                
                // Soft constraints (Restricciones Blandas Dinámicas)
                limiteClasesPorDiaDocente(constraintFactory)
        };
    }

    // Un aula no puede tener dos clases al mismo tiempo
    private Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslot),
                        Joiners.equal(Lesson::getRoom))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflicto de Aula");
    }

    // Un profesor no puede enseñar dos clases al mismo tiempo
    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslot),
                        Joiners.equal(Lesson::getTeacher))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflicto de Profesor");
    }

    // Una sección (grado/grupo) no puede recibir dos clases al mismo tiempo
    private Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslot),
                        Joiners.equal(Lesson::getStudentGroup))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflicto de Seccion Grupo");
    }

    // --- REGLAS DINÁMICAS (Se leen desde ParametrosHorario) ---

    // Un profesor no debería exceder el máximo de clases por día establecido en la configuración
    private Constraint limiteClasesPorDiaDocente(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacher, 
                         lesson -> lesson.getTimeslot().getDayOfWeek(), 
                         ai.timefold.solver.core.api.score.stream.ConstraintCollectors.count())
                .join(com.example.demo.horarios.domain.ParametrosHorario.class)
                .filter((teacher, dayOfWeek, lessonCount, parametros) -> lessonCount > parametros.getMaxClasesPorDiaDocente())
                .penalize(HardSoftScore.ONE_SOFT, 
                         (teacher, dayOfWeek, lessonCount, parametros) -> lessonCount - parametros.getMaxClasesPorDiaDocente())
                .asConstraint("Límite dinámico de clases por día para el docente");
    }
}

