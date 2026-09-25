# TaxRateSystem Backend — Run Guide

## Requirements

* Java 25
* MySQL 8+
* Git

## Run the Application

Clone the repository:

```bash
git clone https://github.com/AndrewSanAntonio1/TaxRateSystem-Backend.git
cd TaxRateSystem-Backend/TaxRateSystem
```

Run the backend:

### Windows

```powershell
.\gradlew bootRun
```

### Linux / macOS

```bash
./gradlew bootRun
```

The backend will run at:

```text
http://localhost:8080
```

## API Base URL

```text
http://localhost:8080/api/v1
```

## Test

```bash
curl http://localhost:8080/
```

If you receive `401 Unauthorized`, the server is running and the endpoint requires authentication.

## Stop the Server

Press:

```text
Ctrl + C
```
