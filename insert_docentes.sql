CREATE TABLE docentes (
    id bigint IDENTITY(1,1) PRIMARY KEY,
    nombre varchar(255) NOT NULL,
    apellido varchar(255) NOT NULL,
    dni varchar(8) NOT NULL UNIQUE,
    email varchar(255) NOT NULL UNIQUE,
    especialidad varchar(255),
    estado varchar(255) NOT NULL DEFAULT 'ACTIVO'
);

INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado) VALUES
('Juan', 'Pérez', '12345678', 'juan.perez@iep.edu.pe', 'Matemática', 'ACTIVO'),
('María', 'Salas', '87654321', 'maria.salas@iep.edu.pe', 'Comunicación', 'ACTIVO'),
('Carlos', 'López', '11223344', 'carlos.lopez@iep.edu.pe', 'Historia', 'ACTIVO'),
('Ana', 'Gómez', '44332211', 'ana.gomez@iep.edu.pe', 'Inglés', 'ACTIVO'),
('Luis', 'Torres', '55667788', 'luis.torres@iep.edu.pe', 'Educación Física', 'ACTIVO'),
('Elena', 'Martínez', '88776655', 'elena.martinez@iep.edu.pe', 'Física', 'ACTIVO'),
('Jorge', 'Sánchez', '99887766', 'jorge.sanchez@iep.edu.pe', 'Química', 'ACTIVO'),
('Rosa', 'Díaz', '66778899', 'rosa.diaz@iep.edu.pe', 'Biología', 'ACTIVO'),
('Miguel', 'Rojas', '22334455', 'miguel.rojas@iep.edu.pe', 'Arte', 'ACTIVO'),
('Carmen', 'Vargas', '55443322', 'carmen.vargas@iep.edu.pe', 'Música', 'ACTIVO'),
('Raúl', 'Fernández', '10203040', 'raul.fernandez@iep.edu.pe', 'Filosofía', 'ACTIVO'),
('Patricia', 'Quispe', '04030201', 'patricia.quispe@iep.edu.pe', 'Psicología', 'ACTIVO'),
('Fernando', 'Condori', '15253545', 'fernando.condori@iep.edu.pe', 'Computación', 'ACTIVO'),
('Lucía', 'Mamani', '54535251', 'lucia.mamani@iep.edu.pe', 'Danza', 'ACTIVO'),
('Hugo', 'Cruz', '16263646', 'hugo.cruz@iep.edu.pe', 'Raz. Matemático', 'ACTIVO');
