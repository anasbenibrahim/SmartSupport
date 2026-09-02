FROM python:3.12-slim

WORKDIR /app

# Installer les dépendances
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copier le code
COPY . .

# Exposer le port
EXPOSE 5000

# Démarrer le service
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "5000"]