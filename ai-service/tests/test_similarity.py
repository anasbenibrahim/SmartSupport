import pytest
import sys
import os

# Ajouter le chemin du projet
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.services.similarity_service import SimilarityService


def test_similarity_service():
    """Teste le service de similarité."""
    service = SimilarityService()
    results = service.find_similar("I cannot login", top_k=3)
    
    assert len(results) <= 3
    assert all("similarity" in r for r in results)
    assert all("text" in r for r in results)