@echo off
chcp 65001 >nul
echo ===== Running Selenium tests (headless) =====
call mvn clean test -Dheadless=true -Dmaven.test.failure.ignore=true %*

echo ===== Generating HTML report =====
call mvn surefire-report:report-only -DlinkXRef=false -q

if exist target\reports\surefire.html (
    start "" target\reports\surefire.html
) else (
    start "" target\site\surefire-report.html
)
if exist target\screenshots (
    echo Screenshots of failed tests: target\screenshots
)
