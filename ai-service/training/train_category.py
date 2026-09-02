import pandas as pd
import joblib
import os

from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix


# Chemins
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_PATH = os.path.join(BASE_DIR, "data", "raw", "tickets.csv")
MODEL_PATH = os.path.join(BASE_DIR, "models", "category_model.joblib")

# Créer le dossier models s'il n'existe pas
os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)

# Charger les données
print("📊 Chargement des données...")
df = pd.read_csv(DATA_PATH)
print(f"   {len(df)} tickets chargés")

X = df["text"]
y = df["category"]

# Division train/test
X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42,
    stratify=y
)

print(f"   Train: {len(X_train)} tickets")
print(f"   Test: {len(X_test)} tickets")

# Créer le pipeline
print("🔧 Création du pipeline...")
model = Pipeline([
    (
        "tfidf",
        TfidfVectorizer(
            lowercase=True,
            stop_words="english",
            ngram_range=(1, 2)
        )
    ),
    (
        "classifier",
        LogisticRegression(
            max_iter=1000,
            random_state=42
        )
    )
])

# Entraîner
print("🚀 Entraînement du modèle...")
model.fit(X_train, y_train)

# Évaluer
print("📈 Évaluation du modèle...")
predictions = model.predict(X_test)

print("\n" + "="*50)
print("CLASSIFICATION REPORT")
print("="*50)
print(classification_report(y_test, predictions))

print("\n" + "="*50)
print("CONFUSION MATRIX")
print("="*50)
print(confusion_matrix(y_test, predictions, labels=model.classes_))
print(f"\nCatégories: {list(model.classes_)}")

# Sauvegarder
joblib.dump(model, MODEL_PATH)
print(f"\n✅ Modèle sauvegardé dans: {MODEL_PATH}")