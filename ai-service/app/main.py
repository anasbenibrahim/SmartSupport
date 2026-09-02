from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router


# Créer l'application FastAPI
app = FastAPI(
    title="SmartSupport AI Service",
    description="Service d'analyse intelligente des tickets de support",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# Configuration CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # À restreindre en production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Inclure les routes
app.include_router(
    router,
    prefix="/api/ai",
    tags=["AI"]
)


@app.get(
    "/",
    summary="Page d'accueil",
    description="Informations générales sur le service"
)
def root():
    """Retourne les informations générales."""
    return {
        "service": "SmartSupport AI Service",
        "version": "1.0.0",
        "documentation": "/docs",
        "health": "/api/ai/health"
    }