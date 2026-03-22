@echo off
setlocal

if "%~1"=="" goto usage

set TARGET=%~1

if /I "%TARGET%"=="start" goto start
if /I "%TARGET%"=="stop" goto stop
if /I "%TARGET%"=="start-prod" goto start_prod
if /I "%TARGET%"=="stop-prod" goto stop_prod
if /I "%TARGET%"=="restart" goto restart
if /I "%TARGET%"=="build" goto build
if /I "%TARGET%"=="rebuild" goto rebuild
if /I "%TARGET%"=="logs" goto logs
if /I "%TARGET%"=="ps" goto ps
if /I "%TARGET%"=="clean" goto clean
if /I "%TARGET%"=="create-admin" goto create_admin
if /I "%TARGET%"=="backend" goto backend
if /I "%TARGET%"=="frontend" goto frontend
if /I "%TARGET%"=="backend-test" goto backend_test
if /I "%TARGET%"=="frontend-build" goto frontend_build

echo Unsupported target: %TARGET%
goto usage

:start
call npm.cmd run start
goto end

:stop
call npm.cmd run stop
goto end

:start_prod
docker compose --env-file runtime.env -f docker-compose.prod.yml up --build
goto end

:stop_prod
docker compose --env-file runtime.env -f docker-compose.prod.yml down
goto end

:restart
call npm.cmd run stop
if errorlevel 1 goto end
call npm.cmd run start
goto end

:build
docker compose --env-file runtime.env build
goto end

:rebuild
docker compose --env-file runtime.env build --no-cache
goto end

:logs
call npm.cmd run logs
goto end

:ps
docker compose --env-file runtime.env ps
goto end

:clean
docker compose --env-file runtime.env down -v
goto end

:create_admin
call npm.cmd run create-admin
goto end

:backend
call npm.cmd run backend
goto end

:frontend
call npm.cmd run frontend
goto end

:backend_test
call npm.cmd run backend:test
goto end

:frontend_build
call npm.cmd run frontend:build
goto end

:usage
echo Usage: make ^<target^>
echo Supported targets: start stop start-prod stop-prod restart build rebuild logs ps clean create-admin backend frontend backend-test frontend-build
exit /b 1

:end
exit /b %errorlevel%
