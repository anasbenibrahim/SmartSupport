from __future__ import annotations

import logging
import pickle
from pathlib import Path

import numpy as np
import pandas as pd
from flask import Flask, jsonify, request
from flask_cors import CORS

BASE_DIR = Path(__file__).resolve().parent
MODELS_FILE = BASE_DIR / "ai_models.pkl"
DATASET_FILE = BASE_DIR / "smart_support_dataset.csv"
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("smart-support-ai")

app = Flask(__name__)
CORS(app)
models: dict = {}
dataset = pd.DataFrame()


def load_artifacts() -> None:
    global models, dataset
    if not MODELS_FILE.exists():
        logger.warning("Model artifact is missing: %s", MODELS_FILE)
        return
    with MODELS_FILE.open("rb") as file:
        models = pickle.load(file)
    if DATASET_FILE.exists():
        dataset = pd.read_csv(DATASET_FILE)
    logger.info("AI artifacts loaded successfully")


def response_suggestion(category: str) -> str:
    suggestions = {
        "TECHNICAL": "Bonjour, nous avons bien reçu votre problème technique. Notre équipe va l'analyser et vous répondre rapidement.",
        "PAYMENT": "Bonjour, nous avons bien reçu votre demande de paiement. Nous allons vérifier la transaction et revenir vers vous.",
        "AUTHENTICATION": "Bonjour, veuillez essayer la réinitialisation de votre mot de passe. Notre équipe vous aidera si le problème persiste.",
        "SUBSCRIPTION": "Bonjour, votre demande concernant l'abonnement est bien enregistrée et sera traitée rapidement.",
        "ACCOUNT": "Bonjour, nous allons vérifier les informations de votre compte et vous répondre prochainement.",
        "OTHER": "Bonjour, merci de nous avoir contactés. Nous allons examiner votre demande et vous répondre prochainement.",
    }
    return suggestions.get(category, suggestions["OTHER"])


def require_models() -> tuple:
    if not models:
        return None, (jsonify({"error": "AI models are not trained yet"}), 503)
    return models, None


@app.get("/health")
def health() -> tuple:
    return jsonify({"status": "UP", "service": "AI-SERVICE", "models_loaded": bool(models)})


@app.post("/api/ai/analyze")
def analyze() -> tuple:
    artifacts, error = require_models()
    if error:
        return error
    payload = request.get_json(silent=True) or {}
    text = str(payload.get("text", "")).strip()
    if not text:
        return jsonify({"error": "Text is required"}), 400
    category_pipeline = artifacts["category_pipeline"]
    priority_pipeline = artifacts["priority_pipeline"]
    category_encoder = artifacts["label_encoder_category"]
    priority_encoder = artifacts["label_encoder_priority"]
    category_encoded = category_pipeline.predict([text])[0]
    priority_encoded = priority_pipeline.predict([text])[0]
    category = category_encoder.inverse_transform([category_encoded])[0]
    priority = priority_encoder.inverse_transform([priority_encoded])[0]
    confidence = float(np.max(category_pipeline.predict_proba([text])[0]))
    return jsonify({"category": category, "priority": priority, "confidence": confidence,
                    "suggested_response": response_suggestion(category), "similar_tickets": []})


@app.post("/api/ai/similar")
def similar() -> tuple:
    artifacts, error = require_models()
    if error:
        return error
    payload = request.get_json(silent=True) or {}
    text = str(payload.get("text", "")).strip()
    if not text:
        return jsonify({"error": "Text is required"}), 400
    if dataset.empty or "text_clean" not in dataset:
        return jsonify({"similar_tickets": []})
    vectorizer = artifacts["category_pipeline"].named_steps["tfidf"]
    query_vector = vectorizer.transform([text])
    dataset_vectors = vectorizer.transform(dataset["text_clean"].fillna(""))
    scores = (dataset_vectors @ query_vector.T).toarray().ravel()
    top_indices = np.argsort(scores)[::-1][:5]
    results = [{"text": dataset.iloc[index]["text_clean"], "category": dataset.iloc[index]["category"],
                "priority": dataset.iloc[index]["priority"], "similarity": round(float(scores[index]), 4)}
               for index in top_indices if scores[index] > 0]
    return jsonify({"similar_tickets": results})


@app.get("/api/ai/metrics")
def metrics() -> tuple:
    artifacts, error = require_models()
    if error:
        return error
    return jsonify({"category_labels": list(artifacts["label_encoder_category"].classes_),
                    "priority_labels": list(artifacts["label_encoder_priority"].classes_),
                    "category_confusion_matrix": artifacts["confusion_matrix_category"],
                    "priority_confusion_matrix": artifacts["confusion_matrix_priority"]})


load_artifacts()

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)