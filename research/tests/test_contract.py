import json
from pathlib import Path

import pytest
from jsonschema import Draft202012Validator, ValidationError

from features.contract import (
    ATOL,
    CONTRACT_NAME,
    FEATURE_DIM,
    FEATURE_GROUPS,
    FEATURE_NAMES,
    GAP_THRESHOLD_MS,
    PALM_SCALE_EPSILON,
    RTOL,
    VECTOR_EPSILON,
    VERSION,
    build_contract,
    build_layout,
    canonical_json,
    contract_sha256,
    validate_contract,
)

REPO_ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = REPO_ROOT / "contracts" / "feature_contract.json"
SCHEMA_PATH = REPO_ROOT / "contracts" / "feature_contract.schema.json"


def test_layout_is_contiguous_and_sized():
    layout = build_layout()
    assert FEATURE_DIM == 256
    assert len(layout) == FEATURE_DIM
    assert [channel["index"] for channel in layout] == list(range(FEATURE_DIM))


def test_feature_names_are_unique():
    names = [channel["name"] for channel in build_layout()]
    assert len(names) == len(set(names))
    assert tuple(names) == FEATURE_NAMES


def test_groups_cover_the_layout_without_overlap():
    cursor = 0
    for group in FEATURE_GROUPS:
        assert group["offset"] == cursor
        assert group["length"] == len(group["channels"])
        assert group["length"] > 0
        cursor += group["length"]
    assert cursor == FEATURE_DIM


def test_contract_constants_are_the_documented_values():
    assert CONTRACT_NAME == "hasta.features"
    assert VERSION == "features-v1"
    assert PALM_SCALE_EPSILON == 1e-6
    assert VECTOR_EPSILON == 1e-9
    assert GAP_THRESHOLD_MS == 600
    assert ATOL == 1e-6
    assert RTOL == 1e-5


def test_contract_json_is_current_and_self_consistent():
    assert CONTRACT_PATH.exists(), "contracts/feature_contract.json must be committed"
    stored = json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))
    assert stored == build_contract()
    validate_contract(stored)


def test_contract_schema_accepts_the_contract():
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    Draft202012Validator.check_schema(schema)
    Draft202012Validator(schema).validate(build_contract())


def test_contract_schema_rejects_an_incompatible_dimension():
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    contract = build_contract()
    contract["feature_dim"] = 255
    with pytest.raises(ValidationError):
        Draft202012Validator(schema).validate(contract)


def test_semantic_validation_rejects_a_gap_in_the_layout():
    contract = build_contract()
    contract["groups"][1]["offset"] += 1
    with pytest.raises(ValueError, match="offset"):
        validate_contract(contract)


def test_semantic_validation_rejects_a_duplicate_channel():
    contract = build_contract()
    contract["groups"][1]["channels"][0] = contract["groups"][0]["channels"][0]
    with pytest.raises(ValueError, match="duplicate"):
        validate_contract(contract)


def test_canonical_json_and_hash_are_stable():
    first = canonical_json(build_contract())
    second = canonical_json(build_contract())
    assert first == second
    assert contract_sha256(build_contract()) == contract_sha256(build_contract())
    assert len(contract_sha256(build_contract())) == 64


def test_every_group_has_a_documented_rule():
    for group in FEATURE_GROUPS:
        assert group["rule"].strip()
        assert group["units"].strip()
