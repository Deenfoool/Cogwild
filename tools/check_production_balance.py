#!/usr/bin/env python3
"""Offline design-only validator; does not load Forge or modify mod content."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "docs/design/production-balance-v0.2.json"
data = json.loads(source.read_text(encoding="utf-8"))
assert data["status"] == "design_baseline_not_implemented"
kinds = set(data["registered_machine_kinds"])
assert len(kinds) == 8 and data["generator"]["machine"] in kinds
assert len(data["process_recipes"]) == 13

names = set()
for recipe in data["process_recipes"]:
    assert recipe["id"] not in names, "duplicate operation"
    names.add(recipe["id"])
    assert recipe["machine"] in kinds
    ticks = recipe["duration_ticks"]
    rate = recipe["fe_per_tick"]
    assert isinstance(ticks, int) and ticks > 0
    assert isinstance(rate, int) and rate > 0
    assert (recipe.get("outputs") or recipe.get("fluid_outputs")), "no output"
    for stack in recipe.get("inputs", []) + recipe.get("outputs", []) + recipe.get("chance_outputs", []):
        assert isinstance(stack["count"], int) and stack["count"] > 0
        assert ":" in stack["item"]
        if "probability" in stack:
            assert 0 <= stack["probability"] <= 1
    for fluid in recipe.get("fluid_inputs", []) + recipe.get("fluid_outputs", []):
        assert isinstance(fluid["mb"], int) and fluid["mb"] > 0
        assert ":" in fluid["fluid"]
    assert (ticks * rate) > 0

by_id = {entry["id"]: entry for entry in data["process_recipes"]}
def output_volume(name):
    return sum(entry["mb"] for entry in by_id[name].get("fluid_outputs", []))
assert output_volume("compress_air") == 1000
assert output_volume("separate_nitrogen") == 400
assert by_id["separate_nitrogen"]["fluid_inputs"][0]["mb"] == 1000
assert by_id["separate_nitrogen"]["vented_fluid_mb"] == 600
assert 1000 == output_volume("separate_nitrogen") + by_id["separate_nitrogen"]["vented_fluid_mb"]
assert by_id["compress_air"]["duration_ticks"] * by_id["compress_air"]["fe_per_tick"] + by_id["separate_nitrogen"]["duration_ticks"] * by_id["separate_nitrogen"]["fe_per_tick"] == 9200
assert by_id["press_vegetable_oil"]["inputs"][0]["count"] * 2 == 16
assert output_volume("press_vegetable_oil") * 2 == 500
assert by_id["quench_steel"]["duration_ticks"] * by_id["quench_steel"]["fe_per_tick"] == 24000

g = data["generator"]
assert g["production_fe_per_tick"] * g["consumption_mb_every_ticks"]["ticks"] == g["generated_fe_per_mb"] * g["consumption_mb_every_ticks"]["mb"]
assert g["production_fe_per_tick"] <= data["existing_runtime"]["network_component_max_fe_per_tick"]
assert data["existing_runtime"]["fuel_generator_fe_per_tick"] < by_id["crusher_iron"]["fe_per_tick"]
print("OK: 8 machines, 13 candidate processing recipes; energy and fluid invariants valid.")
