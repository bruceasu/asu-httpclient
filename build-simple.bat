@echo off
setlocal enabledelayedexpansion
set MAVEN_USER_HOME=c:\Users\suk\.m2
:: set MAVEN_REPO_LOCAL=c:\Users\suk\.m2
set APP_NAME=asu-httpclient
set VERSION=1.0.0
set PROJECT_DIR=%~dp0

set DIST_DIR=dist
set PACKAGE_DIR=%DIST_DIR%\%APP_NAME%
set MAVEN_CMD=
set MAVEN_ARGS=

echo =========================================
echo Building %APP_NAME% Distribution Package
echo =========================================

echo.
echo Cleaning old builds...
rmdir /s /q %DIST_DIR% 2>nul
mkdir %DIST_DIR%
mkdir %PACKAGE_DIR%
:: mkdir %PACKAGE_DIR%\config

call setup-jdk25.bat
if errorlevel 1 (
    echo ERROR: setup-jdk25.bat failed!
    pause
    exit /b 1
)

echo.
echo Validating Java and Maven environment...
java -version
if errorlevel 1 (
    echo ERROR: java is not available after setup-jdk25.bat
    pause
    exit /b 1
)

if exist "%PROJECT_DIR%mvnw.cmd" (
    set MAVEN_CMD=%PROJECT_DIR%mvnw.cmd
) else (
    set MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd
)

call %MAVEN_CMD% -v
if errorlevel 1 (
    echo ERROR: Maven command is not available: %MAVEN_CMD%
    pause
    exit /b 1
)

if not "%MAVEN_REPO_LOCAL%"=="" (
    set MAVEN_ARGS=!MAVEN_ARGS! -Dmaven.repo.local=%MAVEN_REPO_LOCAL%
)
if not "%MAVEN_USER_HOME%"=="" (
    set MAVEN_ARGS=!MAVEN_ARGS! -Duser.home=%MAVEN_USER_HOME%
)

echo.
echo =========================================
echo Step 1/2 - Building Spring Boot JAR
echo =========================================
echo JAVA_HOME     : %JAVA_HOME%
echo MAVEN_HOME    : %MAVEN_HOME%
echo Maven command : %MAVEN_CMD%
echo Maven args    : %MAVEN_ARGS%
echo USERPROFILE   : %USERPROFILE%
echo MAVEN_REPO_LOCAL=%MAVEN_REPO_LOCAL%
echo MAVEN_USER_HOME=%MAVEN_USER_HOME%

call "%MAVEN_CMD%" clean package -DskipTests %MAVEN_ARGS%

if errorlevel 1 (
    echo ERROR: Maven build failed!
    echo Executed: "%MAVEN_CMD%" clean package -DskipTests %MAVEN_ARGS%
    pause
    exit /b 1
)

@rem echo.
@rem echo =========================================
@rem echo Step 2/2 - Copying JAR file
@rem echo =========================================
@rem
@rem for %%f in (target\*.jar) do (
@rem     set JAR=%%f
@rem     set JAR_NAME=%%~nxf
@rem )

@rem if not defined JAR_NAME (
@rem     echo ERROR: No JAR file found in target directory!
@rem     pause
@rem     exit /b 1
@rem )
@rem
@rem echo Copying %JAR_NAME% to package directory...
@rem copy /Y "%JAR%" "%PACKAGE_DIR%\%JAR_NAME%"

pause
