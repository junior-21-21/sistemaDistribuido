@echo off
if "%DB_PASSWORD%"=="" (
  echo Error: configura DB_PASSWORD antes de iniciar los microservicios.
  exit /b 1
)
if "%JWT_SECRET%"=="" (
  echo Error: configura JWT_SECRET con al menos 32 caracteres.
  exit /b 1
)
echo Iniciando todos los microservicios de la plataforma...

echo Levantando API Gateway...
cd api-gateway
start "API Gateway" cmd /c "mvnw.cmd spring-boot:run"
cd ..

echo Levantando Servicio Reportes...
cd servicio-reportes
start "Servicio Reportes" cmd /c "mvnw.cmd spring-boot:run"
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
