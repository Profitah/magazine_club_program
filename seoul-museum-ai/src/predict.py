"""
Model loading and inference utilities for the Seoul museum entrance classifier.
"""

from __future__ import annotations

import io
import json
import logging
from functools import lru_cache
from pathlib import Path
from typing import Dict, List, Optional

import torch
import torch.nn.functional as F
from PIL import Image
from torchvision import models, transforms

LOGGER = logging.getLogger(__name__)

DEFAULT_MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "best_model.pth"
DEFAULT_CLASS_MAP_PATH = Path(__file__).resolve().parent.parent / "models" / "class_mapping.json"


class ModelNotReadyError(RuntimeError):
    """Raised when the inference model cannot be loaded."""


class MuseumClassifier:
    """
    Wrapper that encapsulates preprocessing and inference logic.

    The model checkpoint should be exported using the training notebook:
      * best_model.pth – torch.save dict with `model_state_dict` and `class_names`
      * class_mapping.json – optional text file with class labels
    """

    def __init__(
        self,
        checkpoint_path: Optional[Path] = None,
        class_map_path: Optional[Path] = None,
        device: Optional[str] = None,
    ) -> None:
        self.checkpoint_path = Path(checkpoint_path or DEFAULT_MODEL_PATH)
        self.class_map_path = Path(class_map_path or DEFAULT_CLASS_MAP_PATH)
        self.device = device or ("cuda" if torch.cuda.is_available() else "cpu")
        self.model: Optional[torch.nn.Module] = None
        self.class_names: List[str] = []
        self.preprocess = transforms.Compose(
            [
                transforms.Resize((256, 256)),
                transforms.CenterCrop(224),
                transforms.ToTensor(),
                transforms.Normalize(
                    mean=[0.485, 0.456, 0.406],
                    std=[0.229, 0.224, 0.225],
                ),
            ]
        )

    def load(self) -> None:
        """Load model weights and class metadata into memory."""
        if not self.checkpoint_path.exists():
            raise ModelNotReadyError(f"모델 체크포인트가 존재하지 않습니다: {self.checkpoint_path}")

        checkpoint = torch.load(self.checkpoint_path, map_location=self.device)

        if "class_names" in checkpoint:
            self.class_names = checkpoint["class_names"]
        elif self.class_map_path.exists():
            self.class_names = json.loads(self.class_map_path.read_text(encoding="utf-8"))
        else:
            raise ModelNotReadyError("클래스 정보를 찾을 수 없습니다.")

        model = models.efficientnet_b0(weights=None)
        model.classifier[1] = torch.nn.Linear(model.classifier[1].in_features, len(self.class_names))
        model.load_state_dict(checkpoint["model_state_dict"])
        model.eval()
        model.to(self.device)

        self.model = model
        LOGGER.info("모델 로딩 완료 (device=%s, classes=%d)", self.device, len(self.class_names))

    def predict(self, image_bytes: bytes, topk: int = 3) -> List[Dict[str, float]]:
        """Run inference on the provided image bytes and return Top-K predictions."""
        if self.model is None:
            self.load()

        assert self.model is not None  # for mypy / type checkers

        with Image.open(io.BytesIO(image_bytes)).convert("RGB") as image:
            tensor = self.preprocess(image).unsqueeze(0).to(self.device)

        with torch.no_grad():
            outputs = self.model(tensor)
            probs = F.softmax(outputs, dim=1)
            top_probs, top_indices = probs.topk(min(topk, probs.size(1)))

        flat_probs = top_probs.reshape(-1).tolist()
        flat_indices = top_indices.reshape(-1).tolist()

        results: List[Dict[str, float]] = []
        for prob, idx in zip(flat_probs, flat_indices):
            label = self.class_names[idx]
            results.append({"label": label, "confidence": float(prob)})
        return results


@lru_cache(maxsize=1)
def get_classifier(checkpoint_path: Optional[str] = None, class_map_path: Optional[str] = None) -> MuseumClassifier:
    """Return a cached classifier instance."""
    classifier = MuseumClassifier(
        checkpoint_path=Path(checkpoint_path) if checkpoint_path else None,
        class_map_path=Path(class_map_path) if class_map_path else None,
    )
    classifier.load()
    return classifier


def warmup(classifier: Optional[MuseumClassifier] = None) -> None:
    """
    Perform a warmup forward pass to reduce cold-start latency.
    """
    clf = classifier or get_classifier()
    dummy = Image.new("RGB", (224, 224), (255, 255, 255))
    buf = io.BytesIO()
    dummy.save(buf, format="JPEG")
    clf.predict(buf.getvalue(), topk=1)


