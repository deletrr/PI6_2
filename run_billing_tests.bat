@echo off
echo ============================================================
echo   PontoLivre - Executando Testes e Gerando Relatorio JaCoCo
echo ============================================================
echo.

if not exist "gradlew" (
    echo [ERRO] Arquivo gradlew nao encontrado.
    pause
    exit /b 1
)

:: Limpa builds anteriores e roda os testes gerando o JaCoCo
echo Rodando testes e analisando cobertura de codigo...
echo ------------------------------------------------------------
call ./gradlew :backend:cleanTest :backend:test :backend:jacocoTestReport --info

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo   [SUCESSO] Testes e Analise de Cobertura Concluidos!
    echo ============================================================
    echo.
    echo 1. RELATORIO DE DEBUG (JUnit):
    echo %CD%\backend\build\reports\tests\test\index.html
    echo.
    echo 2. RELATORIO DE COBERTURA (JaCoCo):
    echo %CD%\backend\build\reports\jacoco\index.html
    echo.
    echo (Abra os links acima no seu navegador para ver os resultados)
) else (
    echo.
    echo ============================================================
    echo   [FALHA] Ocorreram erros durante os testes.
    echo ============================================================
)

echo.
pause
