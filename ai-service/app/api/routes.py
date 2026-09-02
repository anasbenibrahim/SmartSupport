from fastapi import APIRouter, HTTPException
import joblib
import os

from app.schemas.ticket_schema import TicketRequest, AnalysisResponse
from app.services.similarity_service import SimilarityService
from app.services.response_service import ResponseService


router = APIRouter()

# Chemins des modèles
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CATEGORY_MODEL_PATH = os.path.join(BASE_DIR, "models", "category_model.joblib")
PRIORITY_MODEL_PATH = os.path.join(BASE_DIR, "models", "priority_model.joblib")

# Chargement des modèles (au démarrage)
print("📥 Chargement des modèles...")
try:
    category_model = joblib.load(CATEGORY_MODEL_PATH)
    priority_model = joblib.load(PRIORITY_MODEL_PATH)
    print("✅ Modèles chargés avec succès")
except FileNotFoundError as e:
    print(f"❌ Erreur: Modèle introuvable - {e}")
    raise

# Initialisation des services
similarity_service = SimilarityService()
response_service = ResponseService()


@router.post(
    "/analyze",
    response_model=AnalysisResponse,
    summary="Analyser un ticket",
    description="Analyse un ticket et retourne la catégorie, priorité, tickets similaires et réponse suggérée"
)
def analyze_ticket(ticket: TicketRequest):
    """
    Analyse un ticket de support.
    """
    # Combiner titre et description
    text = f"{ticket.title} {ticket.description}"
    
    try:
        # Prédiction de la catégorie
        category = category_model.predict([text])[0]
        category_proba = category_model.predict_proba([text])[0]
        confidence = float(max(category_proba))
        
        # Prédiction de la priorité
        priority = priority_model.predict([text])[0]
        
        # Tickets similaires
        similar_tickets = similarity_service.find_similar(text, top_k=3)
        
        # Réponse suggérée
        suggested_response = response_service.generate_response(category)
        
        return {
            "category": category,
            "priority": priority,
            "confidence": confidence,
            "similar_tickets": similar_tickets,
            "suggested_response": suggested_response
        }
    
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Erreur lors de l'analyse: {str(e)}"
        )


@router.get(
    "/categories",
    summary="Liste des catégories",
    description="Retourne la liste des catégories supportées"
)
def get_categories():
    """Retourne les catégories connues par le modèle."""
    if hasattr(category_model, 'classes_'):
        return {"categories": list(category_model.classes_)}
    return {"categories": []}


@router.get(
    "/health",
    summary="État du service",
    description="Vérifie si le service est en ligne"
)
def health_check():
    """Vérifie l'état du service."""
    return {
        "status": "UP",
        "service": "ai-service",
        "version": "1.0.0",
        "models_loaded": True
    }