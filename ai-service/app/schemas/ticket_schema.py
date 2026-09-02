from pydantic import BaseModel, Field


class TicketRequest(BaseModel):
    """
    Modèle de requête pour l'analyse d'un ticket.
    """
    title: str = Field(
        ...,
        min_length=1,
        max_length=200,
        description="Titre du ticket"
    )
    description: str = Field(
        ...,
        min_length=1,
        max_length=2000,
        description="Description détaillée du ticket"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "title": "Cannot login",
                "description": "I cannot access my account since this morning"
            }
        }


class SimilarTicketResponse(BaseModel):
    """Ticket similaire trouvé."""
    text: str
    category: str
    priority: str
    similarity: float


class AnalysisResponse(BaseModel):
    """Réponse d'analyse du ticket."""
    category: str
    priority: str
    confidence: float
    similar_tickets: list[SimilarTicketResponse]
    suggested_response: str