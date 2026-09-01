@echo off
echo Iniciando todos los microservicios de la plataforma...

echo Levantando API Gateway...
cd api-gateway
start "API Gateway" cmd /c "mvnw.cmd spring-boot:run"
cd ..

echo Levantando Servicio Cursos...
cd servicio-cursos
start "Servicio Cursos" cmd /c "mvnw.cmd spring-boot:run"
cd ..

echo Levantando Servicio Disciplina...
cd servicio-disciplina
start "Servicio Disciplina" cmd /c "mvnw.cmd spring-boot:run"
cd ..

echo Levantando Servicio Estudiantes...
cd servicio-estudiantes
start "Servicio Estudiantes" cmd /c "mvnw.cmd spring-boot:run"
cd ..

echo Levantando Servicio Evaluaciones...
cd servicio-evaluaciones
start "Servicio Evaluaciones" cmd /c "mvnw.cmd spring-boot:run"
cd ..

echo Todos los servicios se estan iniciando en ventanas separadas.
pause
