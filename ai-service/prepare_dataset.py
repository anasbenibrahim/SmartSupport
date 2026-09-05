from __future__ import annotations

import pickle
import re
from pathlib import Path

import pandas as pd
from sklearn.model_selection import train_test_split

BASE_DIR = Path(__file__).resolve().parent
INPUT_FILE = BASE_DIR / "customer_support_tickets.csv"
DATASET_FILE = BASE_DIR / "smart_support_dataset.csv"
SPLITS_FILE = BASE_DIR / "dataset_splits.pkl"

CATEGORY_MAPPING = {
    "Technical issue": "TECHNICAL", "Data loss": "TECHNICAL", "Hardware issue": "TECHNICAL",
    "Software bug": "TECHNICAL", "Network problem": "TECHNICAL", "Peripheral compatibility": "TECHNICAL",
    "Product setup": "TECHNICAL", "Installation support": "TECHNICAL", "Display issue": "TECHNICAL",
    "Battery life": "TECHNICAL", "Billing inquiry": "PAYMENT", "Refund request": "PAYMENT",
    "Payment issue": "PAYMENT", "Cancellation request": "SUBSCRIPTION", "Account access": "AUTHENTICATION",
    "Product inquiry": "ACCOUNT", "Product recommendation": "OTHER", "Delivery problem": "OTHER",
}
PRIORITY_MAPPING = {"Critical": "CRITICAL", "High": "HIGH", "Medium": "MEDIUM", "Low": "LOW"}


def clean_text(value: object) -> str:
    if pd.isna(value):
        return ""
    text = str(value).lower()
    text = re.sub(r"\{product_purchased\}", "product", text)
    text = re.sub(r"https?://\S+|www\.\S+|\S+@\S+", " ", text)
    text = re.sub(r"\d{3}[-.]?\d{3}[-.]?\d{4}", " phone ", text)
    text = re.sub(r"[^\w\s]", " ", text)
    text = re.sub(r"\b\d+\b", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def main() -> None:
    if not INPUT_FILE.exists():
        raise FileNotFoundError(f"Dataset not found: {INPUT_FILE}")

    frame = pd.read_csv(INPUT_FILE)
    required = {"Ticket Type", "Ticket Subject", "Ticket Description", "Ticket Priority"}
    missing = required.difference(frame.columns)
    if missing:
        raise ValueError(f"Missing required columns: {sorted(missing)}")

    frame["text_clean"] = (
        frame["Ticket Subject"].fillna("").astype(str) + " " + frame["Ticket Description"].fillna("").astype(str)
    ).map(clean_text)
    frame["category"] = frame["Ticket Type"].map(CATEGORY_MAPPING).fillna("OTHER")
    frame["priority"] = frame["Ticket Priority"].map(PRIORITY_MAPPING).fillna("MEDIUM")
    prepared = frame[["text_clean", "category", "priority"]]
    prepared = prepared[prepared["text_clean"].str.len() > 10].drop_duplicates("text_clean").reset_index(drop=True)
    if len(prepared) < 10:
        raise ValueError("Not enough usable ticket rows after cleaning")

    indices = prepared.index.to_numpy()
    stratify = prepared["category"] if prepared["category"].value_counts().min() >= 2 else None
    train_indices, temporary_indices = train_test_split(indices, test_size=0.2, random_state=42, stratify=stratify)
    temporary_categories = prepared.loc[temporary_indices, "category"]
    temporary_stratify = temporary_categories if temporary_categories.value_counts().min() >= 2 else None
    test_indices, validation_indices = train_test_split(
        temporary_indices, test_size=0.5, random_state=42, stratify=temporary_stratify
    )

    DATASET_FILE.parent.mkdir(parents=True, exist_ok=True)
    prepared.to_csv(DATASET_FILE, index=False)
    with SPLITS_FILE.open("wb") as file:
        pickle.dump({
            "X_train": prepared.loc[train_indices, "text_clean"].to_numpy(),
            "X_test": prepared.loc[test_indices, "text_clean"].to_numpy(),
            "X_val": prepared.loc[validation_indices, "text_clean"].to_numpy(),
            "y_cat_train": prepared.loc[train_indices, "category"].to_numpy(),
            "y_cat_test": prepared.loc[test_indices, "category"].to_numpy(),
            "y_cat_val": prepared.loc[validation_indices, "category"].to_numpy(),
            "y_pri_train": prepared.loc[train_indices, "priority"].to_numpy(),
            "y_pri_test": prepared.loc[test_indices, "priority"].to_numpy(),
            "y_pri_val": prepared.loc[validation_indices, "priority"].to_numpy(),
        }, file)

    print(f"Prepared {len(prepared)} tickets")
    print(f"Categories: {prepared['category'].value_counts().to_dict()}")
    print(f"Priorities: {prepared['priority'].value_counts().to_dict()}")
    print(f"Splits: train={len(train_indices)}, test={len(test_indices)}, validation={len(validation_indices)}")


if __name__ == "__main__":
    main()