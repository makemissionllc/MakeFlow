"""
MakeFlow — Offline training script for the cold-start categorization model.

Run this ONCE, on a regular machine with Python + TensorFlow (not inside the
Android app, not via opencode). It produces a small TFLite model file that
gets bundled into the app as a static asset for Phase A (inference-only,
no on-device training yet).

Setup:
    pip install tensorflow scikit-learn

Usage:
    python train_category_classifier.py
    -> outputs category_classifier.tflite and vocab.json

You MUST expand training_data below with real examples before this is
useful — the ~60 rows here are illustrative seed data covering MakeFlow's
default categories, not a real trained model. Treat this file as a starting
skeleton, not a finished dataset. Realistically you want several hundred to
a few thousand labeled (description -> category) pairs for this to
meaningfully beat the existing substring-match CategorySuggester. Sources:
- Your own default category list (already in DatabaseInitializer)
- Common merchant/description patterns for your target market (Kenya, given
  currency support for KES/UGX/TZS) — e.g. common supermarket names, transport
  terms (matatu, uber, fuel), utility names (KPLC, water)
- Anonymized aggregated patterns if you ever collect opt-in data from real
  users later (a separate, much bigger conversation involving consent and
  privacy commitments — do NOT wire this up without deciding that explicitly)
"""

import json
import numpy as np
import tensorflow as tf
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.model_selection import train_test_split

# ---------------------------------------------------------------------------
# SEED TRAINING DATA — expand this significantly before real use.
# Format: (transaction_description, category_name)
# Categories should match MakeFlow's DatabaseInitializer seed list exactly.
# ---------------------------------------------------------------------------
training_data = [
    ("uber trip", "Public Transport"),
    ("bolt ride", "Public Transport"),
    ("matatu fare", "Public Transport"),
    ("fuel shell", "Fuel"),
    ("total petrol station", "Fuel"),
    ("parking fee", "Parking"),
    ("netflix subscription", "Subscriptions"),
    ("spotify premium", "Subscriptions"),
    ("kplc prepaid tokens", "Utilities"),
    ("nairobi water bill", "Utilities"),
    ("safaricom airtime", "Phone"),
    ("internet bill zuku", "Internet"),
    ("rent payment", "Rent"),
    ("mortgage payment", "Mortgage"),
    ("naivas supermarket", "Groceries"),
    ("carrefour groceries", "Groceries"),
    ("java house coffee", "Restaurant"),
    ("kfc lunch", "Restaurant"),
    ("glovo delivery", "Delivery"),
    ("jumia food order", "Delivery"),
    ("chips and snacks", "Snacks"),
    ("gift for friend", "Gifts"),
    ("salary deposit", "Salary"),
    ("freelance payment received", "Salary"),
    ("bank interest credited", "Interest"),
    ("clothing shop", "Clothing"),
    ("electronics store", "Electronics"),
    ("school fees", "Kids"),
    ("vet bill for dog", "Pets"),
    ("hospital visit", "Healthcare"),
    ("pharmacy medicine", "Healthcare"),
    ("tax payment kra", "Taxes"),
    ("car service maintenance", "Maintenance"),
    ("car insurance premium", "Insurance"),
    ("cinema tickets", "Entertainment"),
    ("gym membership", "Sports"),
    ("hobby craft supplies", "Hobbies"),
    ("holiday flight booking", "Vacations"),
    # Add many more real examples here before training for production use.
]

descriptions = [d.lower().strip() for d, _ in training_data]
labels = [c for _, c in training_data]

# Bag-of-words vectorization — simple, small, fast, interpretable.
# A char n-gram range helps with typos/variants common in free-text notes.
vectorizer = CountVectorizer(analyzer="char_wb", ngram_range=(2, 4), max_features=2000)
X = vectorizer.fit_transform(descriptions).toarray().astype(np.float32)

unique_labels = sorted(set(labels))
label_to_idx = {label: i for i, label in enumerate(unique_labels)}
y = np.array([label_to_idx[label] for label in labels])

if len(training_data) < 30:
    print("WARNING: fewer than 30 examples — this model will not generalize. "
          "Expand training_data before shipping.")

X_train, X_val, y_train, y_val = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y if len(set(y)) > 1 else None
)

# Small feed-forward classifier — deliberately tiny, this is not meant to be
# a heavy model. Keeps the .tflite file small (<1MB) for app bundling.
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(X.shape[1],)),
    tf.keras.layers.Dense(64, activation="relu"),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(len(unique_labels), activation="softmax"),
])
model.compile(optimizer="adam", loss="sparse_categorical_crossentropy", metrics=["accuracy"])
model.fit(X_train, y_train, validation_data=(X_val, y_val), epochs=30, verbose=2)

# Convert to TFLite for on-device inference.
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]  # quantization, smaller file
tflite_model = converter.convert()

with open("category_classifier.tflite", "wb") as f:
    f.write(tflite_model)

# Save the vocabulary + label mapping — the Android side needs this to
# replicate the exact same char n-gram vectorization at inference time.
vocab_data = {
    "vocabulary": vectorizer.vocabulary_,
    "ngram_range": [2, 4],
    "analyzer": "char_wb",
    "labels": unique_labels,  # index-aligned with the model's output layer
}
with open("vocab.json", "w") as f:
    json.dump({k: (v if not isinstance(v, dict) else {kk: int(vv) for kk, vv in v.items()})
               for k, v in vocab_data.items()}, f, indent=2)

print(f"Done. Model: category_classifier.tflite, Vocab: vocab.json")
print(f"Categories ({len(unique_labels)}): {unique_labels}")
print("\nNEXT STEPS:")
print("1. Expand training_data with real examples (aim for 300+ before shipping).")
print("2. Re-run this script to regenerate the .tflite file.")
print("3. Copy both output files into app/src/main/assets/ml/")
print("4. Hand the Android integration prompt to opencode.")
