"""Matriz ejecutable de aceptación transversal para modelos y analítica del motor."""

from __future__ import annotations

import ast
from collections import Counter
from pathlib import Path
from typing import Literal

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


GovernanceCategory = Literal[
    "reproducibility",
    "leakage",
    "calibration",
    "bias",
    "linguisticRobustness",
    "revocation",
    "promotion",
]


class GovernanceCheck(StrictContract):
    """Enlace versionado entre un riesgo, una prueba descubierta y una respuesta segura."""

    category: GovernanceCategory
    component: Version
    testFile: str = Field(pattern=r"^test_[a-z0-9_]+\.py$")
    testMethod: str = Field(pattern=r"^test_[a-z0-9_]+$")
    invariant: str = Field(min_length=20, max_length=300)
    failureResponse: Literal[
        "rejectInput",
        "blockPromotion",
        "safeFallback",
        "suppressOutput",
        "requireHumanReview",
        "stopExperiment",
    ]


class GovernanceAcceptanceMatrix(StrictContract):
    """Exige cobertura completa y referencias únicas a pruebas realmente ejecutables."""

    schemaVersion: Literal[1]
    matrixVersion: Version
    requiredCategories: list[GovernanceCategory] = Field(min_length=7, max_length=7)
    checks: list[GovernanceCheck] = Field(min_length=14)

    @model_validator(mode="after")
    def validate_coverage(self) -> "GovernanceAcceptanceMatrix":
        required = {
            "reproducibility",
            "leakage",
            "calibration",
            "bias",
            "linguisticRobustness",
            "revocation",
            "promotion",
        }
        counts = Counter(check.category for check in self.checks)
        references = [(check.testFile, check.testMethod, check.category) for check in self.checks]
        if set(self.requiredCategories) != required or set(counts) != required:
            raise ValueError("MODEL_GOVERNANCE_CATEGORY_MISSING")
        if any(counts[category] < 2 for category in required) or len(references) != len(set(references)):
            raise ValueError("MODEL_GOVERNANCE_COVERAGE_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "GovernanceAcceptanceMatrix":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))

    def validate_test_references(self, tests_root: Path) -> None:
        """Parsea AST sin importar tests y falla si una evidencia se renombra o desaparece."""
        methods_by_file: dict[str, set[str]] = {}
        for check in self.checks:
            if check.testFile not in methods_by_file:
                path = tests_root / check.testFile
                if not path.is_file() or path.parent.resolve() != tests_root.resolve():
                    raise ValueError("MODEL_GOVERNANCE_TEST_FILE_MISSING")
                tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
                methods_by_file[check.testFile] = {
                    node.name
                    for node in ast.walk(tree)
                    if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
                    and node.name.startswith("test_")
                }
            if check.testMethod not in methods_by_file[check.testFile]:
                raise ValueError("MODEL_GOVERNANCE_TEST_METHOD_MISSING")
