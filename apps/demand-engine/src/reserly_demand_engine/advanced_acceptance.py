"""Matriz ejecutable de aceptación para las decisiones avanzadas de Fase 22."""

from __future__ import annotations

import ast
from collections import Counter
from pathlib import Path
from typing import Literal

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


AdvancedCategory = Literal[
    "optimization", "capacity", "frequency", "equity", "causality", "drift",
    "rollback", "safeDegradation",
]


class AdvancedAcceptanceCheck(StrictContract):
    """Relaciona un riesgo material con una prueba descubierta y respuesta segura."""

    category: AdvancedCategory
    component: Version
    testFile: str = Field(pattern=r"^test_[a-z0-9_]+\.py$")
    testMethod: str = Field(pattern=r"^test_[a-z0-9_]+$")
    invariant: str = Field(min_length=20, max_length=300)
    failureResponse: Literal[
        "disableOptimization", "disablePromotions", "disableAllocation", "rejectDecision",
        "safeFallback", "suppressCausalClaim", "rollbackChampion", "blockRelease",
    ]


class AdvancedDemandAcceptanceMatrix(StrictContract):
    """Falla si una categoría pierde dos evidencias ejecutables o duplica una referencia."""

    schemaVersion: Literal[1]
    matrixVersion: Version
    requiredCategories: list[AdvancedCategory] = Field(min_length=8, max_length=8)
    checks: list[AdvancedAcceptanceCheck] = Field(min_length=16)

    @model_validator(mode="after")
    def validate_coverage(self) -> "AdvancedDemandAcceptanceMatrix":
        required = {
            "optimization", "capacity", "frequency", "equity", "causality", "drift",
            "rollback", "safeDegradation",
        }
        counts = Counter(check.category for check in self.checks)
        references = [(check.testFile, check.testMethod, check.category) for check in self.checks]
        if set(self.requiredCategories) != required or set(counts) != required:
            raise ValueError("ADVANCED_ACCEPTANCE_CATEGORY_MISSING")
        if any(counts[category] < 2 for category in required) or len(references) != len(set(references)):
            raise ValueError("ADVANCED_ACCEPTANCE_COVERAGE_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "AdvancedDemandAcceptanceMatrix":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))

    def validate_test_references(self, tests_root: Path) -> None:
        """Comprueba vía AST que cada evidencia forma parte de la suite unittest descubierta."""
        methods_by_file: dict[str, set[str]] = {}
        for check in self.checks:
            if check.testFile not in methods_by_file:
                path = tests_root / check.testFile
                if not path.is_file() or path.parent.resolve() != tests_root.resolve():
                    raise ValueError("ADVANCED_ACCEPTANCE_TEST_FILE_MISSING")
                tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
                methods_by_file[check.testFile] = {
                    node.name for node in ast.walk(tree)
                    if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
                    and node.name.startswith("test_")
                }
            if check.testMethod not in methods_by_file[check.testFile]:
                raise ValueError("ADVANCED_ACCEPTANCE_TEST_METHOD_MISSING")
