import pandas as pd
import os

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity


class SimilarityService:

    def __init__(self):
        # Chemin vers le dataset
        base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        data_path = os.path.join(base_dir, "data", "raw", "tickets.csv")
        
        self.data = pd.read_csv(data_path)
        
        self.vectorizer = TfidfVectorizer(
            lowercase=True,
            stop_words="english",
            ngram_range=(1, 2)
        )
        
        # Vectoriser tous les tickets existants
        self.vectors = self.vectorizer.fit_transform(self.data["text"])

    def find_similar(self, text: str, top_k: int = 3):
        """
        Trouve les tickets les plus similaires.
        """
        # Vectoriser la requête
        query_vector = self.vectorizer.transform([text])
        
        # Calculer la similarité cosinus
        similarities = cosine_similarity(query_vector, self.vectors)[0]
        
        # Trier par similarité décroissante
        top_indices = similarities.argsort()[::-1][:top_k]
        
        results = []
        for index in top_indices:
            results.append({
                "text": self.data.iloc[index]["text"],
                "category": self.data.iloc[index]["category"],
                "priority": self.data.iloc[index]["priority"],
                "similarity": round(float(similarities[index]), 3)
            })
        
        return results