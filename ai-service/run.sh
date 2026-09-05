#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"
python prepare_dataset.py
python train_model.py
exec gunicorn --bind "0.0.0.0:${PORT:-5000}" --workers "${WORKERS:-2}" --timeout 120 app:app