import pytest
import joblib
import os


BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_PATH = os.path.join(BASE_DIR, "models", "category_model.joblib")


def test_model_exists():
    """Vérifie que le modèle existe."""
    assert os.path.exists(MODEL_PATH), "Le modèle de catégorie n'existe pas"


def test_model_predicts():
    """Vérifie que le modèle peut prédire."""
    model = joblib.load(MODEL_PATH)
    prediction = model.predict(["I cannot login"])
    assert prediction is not None
    assert len(prediction) == 1


def test_categories():
    """Vérifie les catégories supportées."""
    model = joblib.load(MODEL_PATH)
    expected = {"AUTHENTICATION", "PAYMENT", "TECHNICAL", "BUG", "SUBSCRIPTION", "ACCOUNT"}
    actual = set(model.classes_)
    assert expected.issubset(actual)