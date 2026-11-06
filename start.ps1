Write-Host '请先安装 JDK17、Maven、Node.js 和 Python 依赖。'
Start-Process powershell -ArgumentList '-NoExit','-Command','Set-Location backend; mvn spring-boot:run' -WorkingDirectory $PSScriptRoot
Start-Process powershell -ArgumentList '-NoExit','-Command','Set-Location frontend; npm install; npm run dev' -WorkingDirectory $PSScriptRoot
