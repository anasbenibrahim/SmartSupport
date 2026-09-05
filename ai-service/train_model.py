from __future__ import annotations

import pickle
from pathlib import Path

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import LabelEncoder
from sklearn.svm import SVC

BASE_DIR = Path(__file__).resolve().parent
SPLITS_FILE = BASE_DIR / "dataset_splits.pkl"
MODELS_FILE = BASE_DIR / "ai_models.pkl"


def build_category_pipeline() -> Pipeline:
    return Pipeline([("tfidf", TfidfVectorizer(max_features=10000, ngram_range=(1, 2), min_df=1, stop_words="english", sublinear_tf=True)),
                     ("classifier", LogisticRegression(max_iter=1000, C=10, class_weight="balanced", random_state=42))])


def build_priority_pipeline() -> Pipeline:
    return Pipeline([("tfidf", TfidfVectorizer(max_features=10000, ngram_range=(1, 2), min_df=1, stop_words="english", sublinear_tf=True)),
                     ("classifier", SVC(kernel="linear", C=10, probability=True, class_weight="balanced", random_state=42))])


def main() -> None:
    if not SPLITS_FILE.exists():
        raise FileNotFoundError("dataset_splits.pkl is missing; run prepare_dataset.py first")
    with SPLITS_FILE.open("rb") as file:
        splits = pickle.load(file)

    category_encoder = LabelEncoder()
    priority_encoder = LabelEncoder()
    category_train = category_encoder.fit_transform(splits["y_cat_train"])
    category_test = category_encoder.transform(splits["y_cat_test"])
    priority_train = priority_encoder.fit_transform(splits["y_pri_train"])
    priority_test = priority_encoder.transform(splits["y_pri_test"])

    category_pipeline = build_category_pipeline().fit(splits["X_train"], category_train)
    priority_pipeline = build_priority_pipeline().fit(splits["X_train"], priority_train)
    category_predictions = category_pipeline.predict(splits["X_test"])
    priority_predictions = priority_pipeline.predict(splits["X_test"])

    print(f"Category accuracy: {accuracy_score(category_test, category_predictions):.4f}")
    print(f"Priority accuracy: {accuracy_score(priority_test, priority_predictions):.4f}")
    print(classification_report(category_test, category_predictions, target_names=category_encoder.classes_, zero_division=0))
    print(classification_report(priority_test, priority_predictions, target_names=priority_encoder.classes_, zero_division=0))

    artifacts = {
        "category_pipeline": category_pipeline,
        "priority_pipeline": priority_pipeline,
        "label_encoder_category": category_encoder,
        "label_encoder_priority": priority_encoder,
        "confusion_matrix_category": confusion_matrix(category_test, category_predictions).tolist(),
        "confusion_matrix_priority": confusion_matrix(priority_test, priority_predictions).tolist(),
    }
    with MODELS_FILE.open("wb") as file:
        pickle.dump(artifacts, file)

    examples = ["I cannot login to my account", "Payment failed with my credit card", "The application crashes on startup"]
    for example in examples:
        category = category_encoder.inverse_transform([category_pipeline.predict([example])[0]])[0]
        priority = priority_encoder.inverse_transform([priority_pipeline.predict([example])[0]])[0]
        confidence = float(np.max(category_pipeline.predict_proba([example])[0]))
        print(f"{example} -> {category}/{priority} ({confidence:.2%})")


if __name__ == "__main__":
    main()