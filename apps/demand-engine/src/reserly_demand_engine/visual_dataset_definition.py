"""Congela el inventario visual gobernado antes de generar o evaluar activos.

La definición separa entidades de train, validación y test, fija los prompts de
generación y mantiene toda autorización en ``pending``. Este módulo no calcula
predicciones ni abre el test; su salida es exclusivamente un paquete de revisión.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from collections import Counter
from pathlib import Path
from typing import Any
from uuid import NAMESPACE_URL, uuid5


CATEGORIES = (
    "restaurante",
    "peluqueria",
    "campo-de-futbol",
    "pista-de-padel",
    "instalacion-municipal",
    "centro-deportivo",
    "centro-de-estetica",
    "otros",
)

DEVELOPMENT_SCENES = {
    "restaurante": (
        "Mediterranean dining room inside a small sports club, laid tables and an open service pass clearly dominate",
        "neighborhood tapas restaurant with bar counter, dining tables and visible commercial kitchen hatch",
        "hotel-style breakfast restaurant with buffet island and fully set dining area",
        "garden restaurant terrace under a pergola, reserved tables and waiter service station, no sports equipment",
        "contemporary family restaurant with booths, table settings and a clear food-service counter",
    ),
    "peluqueria": (),
    "campo-de-futbol": (
        "indoor five-a-side football pitch with artificial turf, two goals and field markings inside a warehouse",
        "compact urban football cage with artificial grass, goal nets and touchlines between apartment buildings",
        "municipal town football stadium with one full-size grass pitch, goals, corner flags and modest stands",
        "covered football training pitch with artificial turf, penalty boxes and regulation goals",
        "rural amateur football ground with grass, white markings, dugouts and two goals",
    ),
    "pista-de-padel": (
        "indoor padel club court with glass back walls, wire mesh, central net and blue synthetic surface",
        "outdoor panoramic padel court beside a gym building, glass enclosure and central net clearly visible",
        "rooftop padel court at dusk with glass walls, mesh enclosure and floodlights",
        "converted warehouse containing one professional padel court, glass corners and net",
        "small resort padel court with glass enclosure and synthetic turf, separate from nearby leisure areas",
    ),
    "instalacion-municipal": (
        "public civic auditorium with stackable chairs, small stage, accessibility ramp and municipal architecture",
        "town community meeting hall with modular tables, notice boards without readable text and public-service design",
        "municipal library cultural room with bookshelves at the edge, lecture seating and a presentation wall",
        "public neighborhood multipurpose hall prepared for an association meeting, storage chairs and civic finishes",
        "municipal senior community activity room with craft tables, public accessibility features and no gym machines",
    ),
    "centro-deportivo": (
        "modern strength gym floor with squat racks, free weights, cardio zone and rubber flooring",
        "indoor multisport center with basketball hoops, court markings, benches and sports storage",
        "functional training studio with rigs, kettlebells, rowing machines and exercise flooring",
        "municipal fitness center inside a civic building, clearly dominated by training machines and weights",
        "boutique cycling and conditioning studio with exercise bikes, mats and sports equipment",
    ),
    "centro-de-estetica": (
        "professional skincare clinic treatment room with facial bed, magnifying lamp and cosmetic equipment",
        "day spa treatment suite with massage table, towel cabinet and beauty-care trolley",
        "nail and facial beauty center with manicure stations plus a separate facial treatment bed",
        "clinical aesthetic center with non-medical skin-treatment equipment, treatment couch and hygienic cabinetry",
        "beauty waxing and body-treatment room with treatment bed, privacy screen and product trolley",
    ),
    "otros": (
        "bookable coworking workshop with shared desks, whiteboards and a small presentation corner",
        "professional photography studio with seamless backdrop, softboxes, camera stands and prop table",
        "music rehearsal and podcast studio with acoustic panels, microphones, instruments and mixing desk",
        "ceramics workshop with pottery wheels, clay worktables, shelving and kiln area",
        "dance rehearsal studio with sprung floor, mirrored wall and portable sound system, no gym equipment",
    ),
}

TEST_SCENES = {
    "restaurante": (
        "compact fine-dining room above a padel club, set tables and food service are the primary purpose",
        "rustic Spanish grill restaurant with dining tables, service counter and visible kitchen pass",
        "modern vegan cafe restaurant with plated-food display, table seating and commercial service station",
        "coastal seafood restaurant terrace with laid tables, dining chairs and waiter station",
        "family pizzeria dining room with tables, pizza oven behind a service counter and no readable signs",
        "formal municipal cultural-center restaurant, table settings and food-service bar dominate the room",
        "small hotel restaurant prepared for dinner with table linen, place settings and service console",
        "industrial-style tapas dining hall with communal tables and an open kitchen hatch",
        "accessible neighborhood restaurant with booths, reserved tables and a beverage service counter",
        "greenhouse garden restaurant with dining tables, place settings and a discreet food pickup pass",
    ),
    "peluqueria": (
        "unisex hair salon with multiple cutting chairs, full mirrors, wash basins and hood dryers",
        "traditional barber shop with barber chairs, mirrors, clipper stations and hair-washing sink",
        "bright neighborhood hairdresser with styling stations, salon chairs and a visible shampoo area",
        "premium hair color studio with color mixing bar, wash basins and rows of styling mirrors",
        "small salon offering minor manicure service, but haircut chairs, mirrors and wash basins clearly dominate",
        "curly-hair specialist salon with styling chairs, diffusers, mirrors and shampoo bowls",
        "minimalist men's hair studio with three barber chairs, mirrors and grooming tool counters",
        "family hair salon with child and adult cutting chairs, mirrors and hair washing equipment",
        "eco hairdresser with wooden styling stations, salon chairs and backwash basins",
        "large academy-style hair salon floor with many cutting stations, mirrors and wash area",
    ),
    "campo-de-futbol": (
        "full-size outdoor artificial-turf football pitch with two goals, penalty markings and team dugouts",
        "indoor futsal football field with goals and clear football markings inside a multisport complex",
        "small seven-a-side football pitch enclosed by fencing, with goals and artificial grass",
        "historic town football ground with natural grass, goalposts, touchlines and a small covered stand",
        "night football training field under floodlights with turf, multiple goal nets and marked penalty areas",
        "school-community football pitch with regulation goals, center circle and spectator benches",
        "covered rooftop five-a-side football field with artificial turf and goal cages",
        "rural football club pitch with natural grass, corner flags, dugouts and two full goals",
        "urban football cage adjacent to padel courts, football goals and pitch markings dominate",
        "modern indoor football arena with artificial turf, goals, center line and team benches",
    ),
    "pista-de-padel": (
        "single outdoor padel court with four glass walls, wire mesh, central net and blue turf",
        "indoor panoramic padel court in a large sports hall, glass enclosure and net clearly dominate",
        "rooftop sunset padel venue with glass court walls, mesh and central net",
        "forest resort padel court with enclosed glass corners, net and synthetic surface",
        "compact urban padel court beside football cages, with unmistakable glass enclosure and net",
        "premium black-frame padel court inside a warehouse, panoramic glass and blue playing surface",
        "municipal outdoor padel court with green turf, glass back walls, mesh sides and floodlights",
        "coastal padel court protected by glass and wire mesh, with central net and court lines",
        "converted industrial loft containing one enclosed padel court with glass walls and net",
        "small fitness club padel court visible in full, glass box, mesh panels and central net",
    ),
    "instalacion-municipal": (
        "municipal civic hall arranged for a residents meeting, stackable chairs, stage and public accessibility ramp",
        "public library multipurpose room with lecture seating, bookshelves and civic-service interior",
        "town hall event chamber with modular seating, lectern and official but unreadable notice boards",
        "municipal cultural workshop room with craft tables, washable floor and public storage cabinets",
        "neighborhood public association hall with folding tables, stacked chairs and accessibility features",
        "civic auditorium rehearsal room with small stage, audience chairs and municipal building finishes",
        "public youth center activity room with flexible furniture, games storage and community-service layout",
        "municipal senior center social room with card tables, lounge seating and public accessibility design",
        "local government exhibition hall with movable panels, public reception desk and event lighting",
        "public community kitchen classroom with teaching counters, group tables and civic multipurpose design",
    ),
    "centro-deportivo": (
        "large commercial gym with cardio machines, weight racks and dedicated functional training zone",
        "municipal indoor sports center with basketball court, hoops, markings and equipment benches",
        "boutique functional fitness box with rigs, barbells, rowing machines and rubber floor",
        "wellness sports center exercise studio with mats, bikes and resistance equipment, no treatment beds",
        "indoor climbing and conditioning center with climbing wall, exercise mats and training equipment",
        "aquatic sports center dry training gym with treadmills, weights and pool visible through glass",
        "boxing fitness center with ring, heavy bags, training mats and strength equipment",
        "accessible rehabilitation fitness gym with exercise machines and sports training stations, no clinic beds",
        "multisport hall configured for badminton and basketball with nets, hoops and court lines",
        "premium health club gym floor adjacent to spa area, exercise machines and weights clearly dominate",
    ),
    "centro-de-estetica": (
        "facial aesthetics clinic with treatment couch, magnifying lamp, skincare machines and product trolley",
        "body beauty studio with treatment bed, non-medical contouring device and hygienic cabinetry",
        "day spa facial room with massage table, hot towel cabinet and beauty products",
        "nail beauty center with manicure desks, polish displays and one pedicure chair, no haircut stations",
        "mixed beauty salon where facial treatment beds and manicure stations dominate over one minor hair corner",
        "eyebrow and eyelash beauty studio with reclining treatment chairs, ring lamps and cosmetic trolleys",
        "clinical-looking skin care center with cosmetic laser-style equipment and treatment couch, non-medical setting",
        "waxing beauty room with treatment bed, privacy screen, warmer trolley and cosmetic storage",
        "luxury spa treatment suite with massage table, towel shelving and beauty-care workstation",
        "compact facial and makeup studio with reclining beauty chair, magnifying lamp and cosmetic counters",
    ),
    "otros": (
        "bookable coworking meeting loft with shared desks, presentation screen and whiteboards",
        "empty professional photography studio with cyclorama, softboxes, tripods and prop storage",
        "soundproof podcast recording room with microphones, mixing console and acoustic wall panels",
        "ceramics teaching workshop with pottery wheels, clay tables, kiln and shelves of unfinished pieces",
        "dance rehearsal studio with sprung wooden floor, mirrors and ballet barres, no fitness machines",
        "painting and illustration workshop with easels, sinks, art tables and material shelves",
        "small theater rehearsal black-box room with stage lights, movable seating and props",
        "maker workshop with workbenches, safe hand tools, 3D printers and project shelving",
        "event workshop loft arranged for a creative class with long tables and craft supplies",
        "language training classroom for hire with modular desks, presentation screen and learning materials without readable text",
    ),
}

V2_TEST_SCENES = {
    "restaurante": (
        "reservation dining room inside a municipal sports complex, set tables and a staffed-style food pass visually dominate over the distant courts",
        "small chef-led restaurant in a converted workshop, laid tables, open kitchen and plating counter clearly define food service",
        "modern canteen restaurant inside a cultural venue, individual place settings, serving counter and commercial kitchen hatch",
        "rooftop event restaurant prepared for dinner, table linen, place settings, beverage station and enclosed kitchen service area",
        "neighborhood brunch restaurant with display counter, dining booths, espresso station and visible food preparation pass",
        "formal restaurant adjoining a hotel spa, the frame centered on set dining tables and food service with treatment areas absent",
        "family grill restaurant in a football club building, dining tables, open grill pass and service console as the primary function",
        "accessible self-service restaurant with tray counter, hot-food display and a large seated dining zone",
        "intimate tasting restaurant with chef counter, plated place settings and compact professional kitchen behind glass",
        "garden conservatory restaurant configured for reservations, dining tables, place settings and waiter station rather than generic events",
    ),
    "peluqueria": (
        "mixed hair and beauty salon where six haircut chairs, mirrors, wash basins and color stations dominate one small manicure desk",
        "compact barber studio inside a gym complex with barber chairs, clipper counters, mirrors and a shampoo sink",
        "hair color laboratory with mixing counter, hood dryers, backwash bowls and rows of styling stations",
        "curly-hair salon with diffuser stations, hydraulic chairs, mirrors and dedicated shampoo area",
        "wedding hair styling studio with multiple styling chairs, mirrors, dryers and wash basins, no treatment beds",
        "children and family hair salon with cutting chairs of different sizes, mirrors and hair washing equipment",
        "premium unisex hairdresser with long mirror wall, styling trolleys, salon chairs and rear wash zone",
        "traditional men's barber shop in a civic arcade, barber chairs, mirrors and grooming tool stations clearly visible",
        "eco hair salon using wooden furniture but unmistakable backwash sinks, styling chairs, dryers and mirrors",
        "hairdressing academy floor with many practice styling stations, salon chairs, mirrors and shampoo bowls",
    ),
    "campo-de-futbol": (
        "seven-a-side artificial-turf football pitch inside a multi-sport facility, goals, penalty areas and center circle dominate",
        "covered urban futsal field with football goals, touchlines and team benches beside a distant basketball court",
        "full-size natural-grass football training field with goals, corner flags, dugouts and running track outside the touchline",
        "small rooftop five-a-side football cage with artificial turf, two goals and football field markings",
        "coastal amateur football stadium with grass pitch, goal nets, center line and one modest stand",
        "indoor football academy pitch divided by retractable nets, artificial grass, goals and penalty markings",
        "school community football ground with two regulation goals, white lines and spectator shelter",
        "night-time football field under floodlights with artificial turf, goals, corner flags and team dugouts",
        "rural training football pitch with natural grass, goalposts and marked boxes beside a community building",
        "urban football cage adjacent to glass padel courts, football goals and rectangular pitch markings centered in frame",
    ),
    "pista-de-padel": (
        "panoramic padel court inside a municipal sports hall, glass walls, wire mesh and central net fill the frame",
        "single rooftop padel court next to a five-a-side field, unmistakable glass enclosure, mesh sides and net",
        "compact indoor padel club in a converted factory with blue turf, glass corners and central net",
        "outdoor green padel court at a resort, transparent back walls, mesh enclosure and court net clearly visible",
        "coastal padel court protected from wind by panoramic glass, synthetic turf and central net",
        "black-frame competition padel court under arena lighting with full glass box and spectator benches outside",
        "community padel court beside a civic center, glass back walls, mesh panels, net and floodlights",
        "forest lodge padel court with enclosed glass corners, wire fencing and blue playing surface",
        "small fitness-club padel court seen from an elevated angle, four glass walls, mesh and central net",
        "covered open-sided padel court with synthetic turf, glass back walls and net, distinct from nearby tennis courts",
    ),
    "instalacion-municipal": (
        "public town-hall multipurpose chamber with civic dais, stackable audience chairs and accessibility ramp, no sports equipment",
        "municipal library lecture room with perimeter bookshelves, presentation seating and public-service interior",
        "neighborhood civic association hall with folding tables, stacked chairs, notice boards without readable text and accessible entrance",
        "public cultural auditorium with small stage, retractable seating and municipal architectural finishes",
        "municipal youth center activity hall with flexible tables, community games storage and public reception corner",
        "public senior center social room with card tables, lounge seating and clearly accessible civic design",
        "town cultural center craft classroom with washable worktables, public storage cabinets and shared sinks",
        "municipal exhibition and meeting gallery with movable panels, lectern and rows of event chairs",
        "public community teaching kitchen with group worktables, demonstration counters and civic multipurpose layout",
        "local-government rehearsal hall with stage curtains, audience chairs and public building accessibility features",
    ),
    "centro-deportivo": (
        "municipal gym inside a civic building, strength machines, free weights and cardio equipment dominate the public architecture",
        "multi-sport training center with basketball court, climbing wall and conditioning zone visible in one coherent hall",
        "boxing fitness club with ring, heavy bags, strength racks and rubber training floor",
        "aquatic sports center dry gym with rowing machines, weights and swimming pool visible only through a distant window",
        "functional fitness warehouse with rigs, barbells, sled track, rowing machines and exercise flooring",
        "accessible rehabilitation sports gym with resistance machines, parallel training bars and exercise stations, no treatment beds",
        "premium health-club exercise floor beside a spa corridor, weights and cardio machines clearly define the main use",
        "indoor cycling and strength studio with exercise bikes, racks, kettlebells and mirrored training area",
        "large racket-sports conditioning gym with treadmills, cable machines and free-weight zone, courts outside the room",
        "community indoor sports hall configured for basketball and badminton with hoops, nets, court lines and equipment benches",
    ),
    "centro-de-estetica": (
        "mixed beauty and hair venue where facial treatment couches, skincare machines and manicure stations dominate one minor styling chair",
        "professional facial clinic with reclining treatment beds, magnifying lamps, cosmetic devices and hygienic cabinetry",
        "body aesthetics studio with treatment couch, non-medical contouring equipment, privacy screen and product trolley",
        "eyebrow and eyelash beauty studio with reclining chairs, ring lamps, cosmetic carts and no haircut basins",
        "nail and pedicure beauty center with manicure desks, pedicure chairs and polish displays, no barber equipment",
        "day spa treatment suite with massage table, hot-towel cabinet, beauty products and private changing corner",
        "clinical-style skin care center with cosmetic light equipment, treatment bed and sterile-looking storage, non-medical context",
        "waxing and body-care room with treatment couch, warmer trolley, privacy screen and cosmetic shelving",
        "luxury facial treatment center with multiple private cabins, beauty beds, magnifying lamps and reception at the edge",
        "makeup and skincare studio with reclining beauty chairs, illuminated mirrors and cosmetic workstations, no shampoo sinks",
    ),
    "otros": (
        "private ceramics school for bookings with pottery wheels, clay worktables, kiln and shelves of unfinished pieces, not a civic hall",
        "commercial photography rental studio with white cyclorama, softboxes, tripods, cameras and prop racks",
        "professional podcast and music recording studio with microphones, mixing console, instruments and acoustic panels",
        "maker-space workshop for private classes with workbenches, 3D printers, electronics tools and project shelves",
        "black-box theater rehearsal studio with stage lighting grid, props, marked performance floor and movable audience risers",
        "artist painting workshop with easels, canvases, wash sinks, material carts and individual workstations",
        "culinary photography production studio with camera rig, backdrop table and lighting equipment, no dining service area",
        "dance rehearsal rental studio with sprung floor, ballet barres, full mirrors and sound console, no gym machines",
        "escape-room production workshop with themed props, control desk and modular scenic panels, clearly a private creative venue",
        "professional coworking innovation lab with reservable project desks, prototype equipment and glass meeting pods, not public municipal",
    ),
}

V2_COMMON_PROMPT = (
    "Use case: photorealistic-natural. Asset type: independent visual classification stress-test image. "
    "Create one photorealistic landscape 4:3 commercial-property photograph of a unique empty venue in Spain. "
    "Show one coherent wide-angle scene with realistic natural or architectural lighting and credible materials. "
    "No people, no readable text, no logos, no watermarks, no collage, no split screen and no duplicated room. "
    "The intended category must remain visually defensible despite neighboring-category context. Scene: "
)

COMMON_PROMPT = (
    "Create one photorealistic landscape 4:3 commercial-property photograph of an empty venue in Spain. "
    "No people, no readable text, no logos, no watermarks, no collage and no split screen. "
    "The image must show a single coherent venue with realistic lighting and wide-angle composition. Scene: "
)


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    """Carga un JSONL UTF-8 y falla ante líneas vacías o JSON inválido."""

    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _generated_row(category: str, split: str, index: int, scene: str) -> dict[str, Any]:
    """Crea una entidad sintética nueva con identidad estable y autorización cerrada."""

    key = f"{split}/{category}/{index:02d}"
    filename = f"images/{split}-{category}-{index:02d}.png"
    return {
        "imageId": str(uuid5(NAMESPACE_URL, f"reserly-visual-image/{key}")),
        "venueId": str(uuid5(NAMESPACE_URL, f"reserly-visual-venue/{key}")),
        "categoryCode": category,
        "split": split,
        "relativePath": filename,
        "source": "generated-v1",
        "variant": scene,
        "prompt": COMMON_PROMPT + scene + ".",
        "generatorProvenance": {
            "provider": "openai-built-in-imagegen",
            "modelKey": "managed-built-in",
            "modelRevision": "notExposedByProvider",
            "promptVersion": "visual-training-balanced-hard-negatives-v1",
        },
        "humanReviewStatus": "pending",
        "developmentTrainingAllowed": False,
        "productionTrainingAllowed": False,
    }


def freeze_definition(dataset_dir: Path, output_dir: Path) -> dict[str, Any]:
    """Fija 80/40/80 filas balanceadas sin observar resultados del modelo."""

    active_assets = _read_jsonl(dataset_dir / "image-assets.v2-development.jsonl")
    by_category = {
        category: sorted(
            (row for row in active_assets if row["categoryCode"] == category),
            key=lambda row: row["objectKey"],
        )
        for category in CATEGORIES
    }
    rows: list[dict[str, Any]] = []
    for category in CATEGORIES:
        if len(by_category[category]) < 10:
            raise ValueError(f"VISUAL_TRAINING_SOURCE_INSUFFICIENT:{category}")
        for split, selected in (
            ("train", by_category[category][:10]),
            ("validation", by_category[category][10:15]),
        ):
            for asset in selected:
                relative = asset["objectKey"].removeprefix(
                    "local-dev://synthetic-marketplace-v1/"
                )
                rows.append(
                    {
                        "imageId": str(uuid5(NAMESPACE_URL, asset["objectKey"])),
                        "venueId": asset["venueId"],
                        "categoryCode": category,
                        "split": split,
                        "relativePath": f"../{relative}",
                        "source": "existing-active-v2",
                        "sourceSha256": asset["sha256"],
                        "humanReviewStatus": "pending",
                        "developmentTrainingAllowed": False,
                        "productionTrainingAllowed": False,
                    }
                )
        missing_validation = 5 - len(by_category[category][10:15])
        scenes = DEVELOPMENT_SCENES[category]
        if missing_validation > len(scenes):
            raise ValueError(f"VISUAL_TRAINING_SCENES_INSUFFICIENT:{category}")
        rows.extend(
            _generated_row(category, "validation", index, scene)
            for index, scene in enumerate(scenes[:missing_validation], start=1)
        )
        rows.extend(
            _generated_row(category, "test", index, scene)
            for index, scene in enumerate(TEST_SCENES[category], start=1)
        )
    counts = Counter((row["split"], row["categoryCode"]) for row in rows)
    expected = {"train": 10, "validation": 5, "test": 10}
    if len(rows) != 200 or any(
        counts[(split, category)] != count
        for split, count in expected.items()
        for category in CATEGORIES
    ):
        raise ValueError("VISUAL_TRAINING_SPLIT_CONTRACT_INVALID")
    definition = {
        "schemaVersion": 1,
        "datasetVersion": "visual-category-dataset-v1",
        "frozenAt": "2026-08-28T00:00:00Z",
        "testOpenedAtFreeze": False,
        "testEvaluationBudget": 1,
        "containsPersonalData": False,
        "synthetic": True,
        "sourceHoldoutConsumed": False,
        "humanReviewRequired": True,
        "automaticAuthorizationAllowed": False,
        "splitContract": {
            "trainPerCategory": 10,
            "validationPerCategory": 5,
            "testPerCategory": 10,
        },
        "categories": list(CATEGORIES),
        "rows": rows,
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "definition.json").write_text(
        json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    worklist = [row for row in rows if row["source"] == "generated-v1"]
    (output_dir / "generation-worklist.jsonl").write_text(
        "".join(json.dumps(row, ensure_ascii=False) + "\n" for row in worklist),
        encoding="utf-8",
    )
    return definition


def freeze_materialized_provisional_definition(
    dataset_dir: Path, output_dir: Path
) -> dict[str, Any]:
    """Congela un experimento menor usando solo activos ya materializados.

    Se preserva un test mayor que validación (7 frente a 3 por clase) y se
    excluye cualquier activo no disponible. La salida no satisface el contrato
    definitivo de 200 imágenes y por ello se identifica como provisional.
    """

    active_assets = _read_jsonl(dataset_dir / "image-assets.v2-development.jsonl")
    target = json.loads((output_dir / "definition.json").read_text(encoding="utf-8"))
    generated_lookup = {
        row["relativePath"]: row
        for row in target["rows"]
        if row["source"] == "generated-v1"
        and (output_dir / row["relativePath"]).is_file()
    }
    by_category = {
        category: sorted(
            (row for row in active_assets if row["categoryCode"] == category),
            key=lambda row: row["objectKey"],
        )
        for category in CATEGORIES
    }

    def existing_row(asset: dict[str, Any], split: str) -> dict[str, Any]:
        relative = asset["objectKey"].removeprefix(
            "local-dev://synthetic-marketplace-v1/"
        )
        return {
            "imageId": str(uuid5(NAMESPACE_URL, asset["objectKey"])),
            "venueId": asset["venueId"],
            "categoryCode": asset["categoryCode"],
            "split": split,
            "relativePath": f"../{relative}",
            "source": "existing-active-v2",
            "sourceSha256": asset["sha256"],
            "humanReviewStatus": "pending",
            "developmentTrainingAllowed": False,
            "productionTrainingAllowed": False,
        }

    def generated_rows(category: str, prefix: str) -> list[dict[str, Any]]:
        pattern = re.compile(rf"^images/{prefix}-{re.escape(category)}-\d{{2}}\.png$")
        return [
            generated_lookup[key]
            for key in sorted(generated_lookup)
            if pattern.match(key)
        ]

    rows: list[dict[str, Any]] = []
    for category in CATEGORIES:
        existing = by_category[category]
        test_generated = generated_rows(category, "test")
        validation_generated = generated_rows(category, "validation")
        rows.extend(existing_row(asset, "train") for asset in existing[:5])
        rows.extend(existing_row(asset, "validation") for asset in existing[5:8])
        if len(test_generated) >= 7:
            selected_test = test_generated[:7]
        elif len(validation_generated) >= 5:
            selected_test = validation_generated[:5] + [
                existing_row(asset, "test") for asset in existing[8:10]
            ]
        else:
            raise ValueError(f"VISUAL_PROVISIONAL_TEST_INSUFFICIENT:{category}")
        for row in selected_test:
            if row["source"] == "generated-v1":
                row = {**row, "split": "test"}
            rows.append(row)

    counts = Counter((row["split"], row["categoryCode"]) for row in rows)
    expected = {"train": 5, "validation": 3, "test": 7}
    if len(rows) != 120 or any(
        counts[(split, category)] != count
        for split, count in expected.items()
        for category in CATEGORIES
    ):
        raise ValueError("VISUAL_PROVISIONAL_SPLIT_CONTRACT_INVALID")
    for row in rows:
        image_path = (output_dir / row["relativePath"]).resolve()
        row["imageSha256"] = hashlib.sha256(image_path.read_bytes()).hexdigest()
    if len({row["imageId"] for row in rows}) != 120 or len(
        {row["venueId"] for row in rows}
    ) != 120 or len({row["imageSha256"] for row in rows}) != 120:
        raise ValueError("VISUAL_PROVISIONAL_LEAKAGE_OR_DUPLICATE")
    definition = {
        "schemaVersion": 1,
        "datasetVersion": "visual-category-dataset-v1-provisional-120",
        "frozenAt": "2026-08-28T00:00:00Z",
        "status": "awaiting_human_review",
        "definitiveContractSatisfied": False,
        "definitiveContractShortfall": 80,
        "generationStoppedByUser": True,
        "testOpenedAtFreeze": False,
        "testEvaluationBudget": 1,
        "containsPersonalData": False,
        "synthetic": True,
        "sourceHoldoutConsumed": False,
        "humanReviewRequired": True,
        "automaticAuthorizationAllowed": False,
        "splitContract": {
            "trainPerCategory": 5,
            "validationPerCategory": 3,
            "testPerCategory": 7,
        },
        "categories": list(CATEGORIES),
        "rows": rows,
    }
    (output_dir / "provisional-definition.json").write_text(
        json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return definition


def freeze_definitive_v2(
    approved_development_path: Path, output_dir: Path
) -> dict[str, Any]:
    """Congela 120 activos consumidos como desarrollo y 80 tests v2 inéditos.

    El test provisional anterior deja de ser test y se incorpora únicamente a
    train/validación. Ningún píxel de aquel test reaparece en el nuevo test v2.
    """

    approved = json.loads(approved_development_path.read_text(encoding="utf-8"))
    if (
        approved.get("datasetVersion") != "visual-category-dataset-v1-provisional-120"
        or approved.get("status") != "approved_for_provisional_training"
        or len(approved.get("rows", [])) != 120
        or any(
            row.get("humanReviewStatus") != "approved"
            or row.get("developmentTrainingAllowed") is not True
            for row in approved["rows"]
        )
    ):
        raise ValueError("VISUAL_V2_DEVELOPMENT_NOT_AUTHORIZED")
    rows: list[dict[str, Any]] = []
    source_definition_dir = approved_development_path.parent.resolve()
    target_definition_dir = output_dir.resolve()

    def development_row(row: dict[str, Any], split: str) -> dict[str, Any]:
        """Reubica una ruta v1 respecto al directorio v2 sin mover el activo."""

        source_path = (source_definition_dir / row["relativePath"]).resolve()
        relative = Path(os.path.relpath(source_path, target_definition_dir)).as_posix()
        return {
            **row,
            "split": split,
            "relativePath": relative,
            "v2Role": "development-from-consumed-provisional",
        }

    for category in CATEGORIES:
        category_rows = [
            row for row in approved["rows"] if row["categoryCode"] == category
        ]
        previous_train = [row for row in category_rows if row["split"] == "train"]
        previous_validation = [
            row for row in category_rows if row["split"] == "validation"
        ]
        consumed_test = [row for row in category_rows if row["split"] == "test"]
        if not (
            len(previous_train) == 5
            and len(previous_validation) == 3
            and len(consumed_test) == 7
        ):
            raise ValueError(f"VISUAL_V2_DEVELOPMENT_SPLIT_INVALID:{category}")
        train_rows = previous_train + consumed_test[:5]
        validation_rows = previous_validation + consumed_test[5:]
        rows.extend(development_row(row, "train") for row in train_rows)
        rows.extend(development_row(row, "validation") for row in validation_rows)
        for index, scene in enumerate(V2_TEST_SCENES[category], start=1):
            key = f"visual-category-dataset-v2/test/{category}/{index:02d}"
            rows.append(
                {
                    "imageId": str(uuid5(NAMESPACE_URL, f"reserly-image/{key}")),
                    "venueId": str(uuid5(NAMESPACE_URL, f"reserly-venue/{key}")),
                    "categoryCode": category,
                    "split": "test",
                    "relativePath": f"images/test-v2-{category}-{index:02d}.png",
                    "source": "generated-independent-test-v2",
                    "v2Role": "unopened-independent-test",
                    "variant": scene,
                    "prompt": V2_COMMON_PROMPT + scene + ".",
                    "generatorProvenance": {
                        "provider": "openai-built-in-imagegen",
                        "modelKey": "managed-built-in",
                        "modelRevision": "notExposedByProvider",
                        "promptVersion": "visual-training-independent-hard-negatives-v2",
                    },
                    "humanReviewStatus": "pending",
                    "developmentTrainingAllowed": False,
                    "productionTrainingAllowed": False,
                }
            )
    counts = Counter((row["split"], row["categoryCode"]) for row in rows)
    expected = {"train": 10, "validation": 5, "test": 10}
    if len(rows) != 200 or any(
        counts[(split, category)] != count
        for split, count in expected.items()
        for category in CATEGORIES
    ):
        raise ValueError("VISUAL_V2_SPLIT_CONTRACT_INVALID")
    if len({row["imageId"] for row in rows}) != 200 or len(
        {row["venueId"] for row in rows}
    ) != 200:
        raise ValueError("VISUAL_V2_ENTITY_LEAKAGE")
    definition = {
        "schemaVersion": 1,
        "datasetVersion": "visual-category-dataset-v2-definitive-200",
        "frozenAt": "2026-08-29T00:00:00+02:00",
        "status": "awaiting_test_materialization_and_human_review",
        "definitiveContractSatisfied": False,
        "developmentRowsApproved": 120,
        "testRowsPending": 80,
        "previousProvisionalTestConsumedAsDevelopment": True,
        "independentFromPreviousTests": True,
        "testOpenedAtFreeze": False,
        "testEvaluationBudget": 1,
        "containsPersonalData": False,
        "synthetic": True,
        "humanReviewRequired": True,
        "automaticAuthorizationAllowed": False,
        "splitContract": {
            "trainPerCategory": 10,
            "validationPerCategory": 5,
            "testPerCategory": 10,
        },
        "categories": list(CATEGORIES),
        "rows": rows,
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "definition.json").write_text(
        json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    test_worklist = [row for row in rows if row["split"] == "test"]
    (output_dir / "generation-worklist.jsonl").write_text(
        "".join(json.dumps(row, ensure_ascii=False) + "\n" for row in test_worklist),
        encoding="utf-8",
    )
    return definition


def run() -> None:
    """CLI para materializar la definición congelada y la cola de generación."""

    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--materialized-provisional", action="store_true")
    parser.add_argument("--definitive-v2-approved-development", type=Path)
    args = parser.parse_args()
    if args.materialized_provisional and args.definitive_v2_approved_development:
        raise ValueError("VISUAL_DEFINITION_MODE_CONFLICT")
    if args.definitive_v2_approved_development:
        definition = freeze_definitive_v2(
            args.definitive_v2_approved_development, args.output_dir
        )
    elif args.materialized_provisional:
        definition = freeze_materialized_provisional_definition(
            args.dataset_dir, args.output_dir
        )
    else:
        definition = freeze_definition(args.dataset_dir, args.output_dir)
    print(json.dumps({"rows": len(definition["rows"]), "frozen": True}))


if __name__ == "__main__":
    run()
