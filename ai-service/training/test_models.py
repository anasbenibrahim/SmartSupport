import joblib
import os


BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATEGORY_MODEL_PATH = os.path.join(BASE_DIR, "models", "category_model.joblib")
PRIORITY_MODEL_PATH = os.path.join(BASE_DIR, "models", "priority_model.joblib")

print("📥 Chargement des modèles...")
category_model = joblib.load(CATEGORY_MODEL_PATH)
priority_model = joblib.load(PRIORITY_MODEL_PATH)

texts = [
    "I cannot login to my account",
    "My card was charged twice",
    "The application crashes",
    "I want to cancel my subscription",
    "The button does not work"
]

print("\n" + "="*60)
print("RÉSULTATS DES PRÉDICTIONS")
print("="*60)

for text in texts:
    category = category_model.predict([text])[0]
    priority = priority_model.predict([text])[0]
    
    # Probabilités
    category_proba = category_model.predict_proba([text])[0]
    confidence = max(category_proba)
    
    print(f"\n📝 Ticket: {text}")
    print(f"   Catégorie: {category} (confiance: {confidence:.2f})")
    print(f"   Priorité: {priority}")