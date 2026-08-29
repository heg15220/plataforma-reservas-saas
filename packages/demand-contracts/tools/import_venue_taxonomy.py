"""Importa el Excel normalizado a un catálogo candidato reproducible.

No ejecuta macros, fórmulas ni instrucciones contenidas en el libro. Lee únicamente
las columnas funcionales de jerarquía, etiqueta y uso necesarias para el producto.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import unicodedata
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


MAIN_NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL_NS = {"r": "http://schemas.openxmlformats.org/package/2006/relationships"}
DOC_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

FAMILY_I18N = {
    "Restauración y bebidas": ("Food and drink", "Locales dedicados a restauración y bebidas.", "Venues serving food and drinks."),
    "Comercio alimentario": ("Food retail", "Comercios minoristas de alimentación y bebidas.", "Retail venues selling food and drinks."),
    "Moda y complementos": ("Fashion and accessories", "Comercios de moda, calzado y complementos.", "Fashion, footwear and accessories retailers."),
    "Farmacia, cosmética y salud retail": ("Pharmacy, cosmetics and health retail", "Comercio sanitario, cosmético y de bienestar minorista.", "Retail pharmacies, cosmetics and consumer health venues."),
    "Hogar y bricolaje": ("Home and DIY", "Comercios de hogar, mobiliario, decoración y bricolaje.", "Home, furniture, decoration and DIY retailers."),
    "Tecnología y oficina": ("Technology and office", "Comercios de tecnología, comunicaciones y material de oficina.", "Technology, communications and office-supply retailers."),
    "Comercio cultural y especializado": ("Culture and specialist retail", "Comercio cultural y minorista especializado.", "Cultural and specialist retail venues."),
    "Flores, jardinería y mascotas": ("Flowers, gardening and pets", "Comercios de floristería, jardinería y productos para mascotas.", "Flower, gardening and pet-supply retailers."),
    "Automoción y movilidad": ("Automotive and mobility", "Comercio y servicios vinculados a vehículos y movilidad.", "Retail and services related to vehicles and mobility."),
    "Grandes superficies y comercio general": ("Department stores and general retail", "Grandes superficies y comercios de surtido general.", "Department stores and general-assortment retailers."),
    "Alojamiento": ("Accommodation", "Establecimientos de alojamiento temporal.", "Venues providing temporary accommodation."),
    "Salud y clínicas": ("Health and clinics", "Centros y consultas de atención sanitaria.", "Healthcare centres and clinical practices."),
    "Veterinaria y cuidado animal": ("Veterinary and animal care", "Centros veterinarios y servicios de cuidado animal.", "Veterinary and animal-care venues."),
    "Servicios sociales": ("Social services", "Centros residenciales, asistenciales y de cuidado social.", "Residential, assistance and social-care venues."),
    "Belleza y cuidado personal": ("Beauty and personal care", "Locales de belleza, bienestar y cuidado personal no clínico.", "Non-clinical beauty, wellness and personal-care venues."),
    "Deporte y actividad física": ("Sport and physical activity", "Instalaciones y centros para deporte y actividad física.", "Facilities and centres for sport and physical activity."),
    "Educación y formación": ("Education and training", "Centros educativos, academias y espacios de formación.", "Educational centres, academies and training venues."),
    "Ocio, cultura y entretenimiento": ("Leisure, culture and entertainment", "Espacios culturales, recreativos y de entretenimiento.", "Cultural, recreational and entertainment venues."),
    "Fotografía y reparaciones": ("Photography and repairs", "Estudios fotográficos y talleres de reparación.", "Photography studios and repair workshops."),
    "Viajes, alquiler y movilidad": ("Travel, rental and mobility", "Agencias, alquileres y servicios de movilidad.", "Travel agencies, rentals and mobility services."),
    "Servicios profesionales y empresas": ("Professional and business services", "Despachos, oficinas y centros de servicios profesionales.", "Offices and venues providing professional business services."),
    "Finanzas, seguros e inmobiliario": ("Finance, insurance and real estate", "Oficinas financieras, aseguradoras e inmobiliarias.", "Financial, insurance and real-estate offices."),
    "Otros servicios al público": ("Other public-facing services", "Otros establecimientos de servicio presencial al público.", "Other venues providing in-person public services."),
}

LEGACY = [
    ("restaurante", "canonicalType", ["restaurante"], None, "exact"),
    ("peluqueria", "canonicalType", ["peluqueria"], None, "exact"),
    ("campo-de-futbol", "canonicalType", ["campo-instalacion-de-futbol"], None, "exact"),
    ("pista-de-padel", "canonicalType", ["club-pistas-de-padel"], None, "exact"),
    ("instalacion-municipal", "operatorAttribute", [], "public-municipal", "requiresReclassification"),
    ("centro-deportivo", "canonicalType", ["gimnasio-centro-deportivo", "instalacion-deportiva"], None, "partial"),
    ("centro-de-estetica", "canonicalType", ["salon-centro-de-belleza", "centro-de-estetica-no-medica"], None, "partial"),
    ("otros", "compositeRequiresReview", ["estudio-fotografico", "coworking-oficina-flexible-centro-de-negocios"], None, "requiresReclassification"),
]


def slug(value: str) -> str:
    """Produce códigos estables legibles sin depender del locale del sistema."""
    normalized = unicodedata.normalize("NFKD", value)
    ascii_value = "".join(char for char in normalized if not unicodedata.combining(char))
    return re.sub(r"[^a-z0-9]+", "-", ascii_value.casefold()).strip("-")


def _cell_value(cell: ET.Element, shared: list[str]) -> object:
    kind = cell.attrib.get("t")
    if kind == "inlineStr":
        return "".join(node.text or "" for node in cell.findall(".//m:t", MAIN_NS))
    value = cell.find("m:v", MAIN_NS)
    if value is None or value.text is None:
        return None
    if kind == "s":
        return shared[int(value.text)]
    try:
        number = float(value.text)
        return int(number) if number.is_integer() else number
    except ValueError:
        return value.text


def read_rows(source: Path) -> list[list[object]]:
    """Lee la primera hoja del paquete OpenXML sin evaluar fórmulas ni contenido activo."""
    with zipfile.ZipFile(source) as archive:
        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        rels = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        rel_by_id = {item.attrib["Id"]: item.attrib["Target"] for item in rels.findall("r:Relationship", REL_NS)}
        shared: list[str] = []
        if "xl/sharedStrings.xml" in archive.namelist():
            shared_root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
            shared = ["".join(node.text or "" for node in item.findall(".//m:t", MAIN_NS)) for item in shared_root.findall("m:si", MAIN_NS)]
        sheet = workbook.find("m:sheets/m:sheet", MAIN_NS)
        relation = sheet.attrib[f"{{{DOC_REL}}}id"]
        target = rel_by_id[relation].lstrip("/")
        sheet_path = target if target.startswith("xl/") else f"xl/{target}"
        root = ET.fromstring(archive.read(sheet_path))
        rows: list[list[object]] = []
        for row in root.findall(".//m:sheetData/m:row", MAIN_NS):
            values: dict[int, object] = {}
            for cell in row.findall("m:c", MAIN_NS):
                letters = re.match(r"[A-Z]+", cell.attrib["r"]).group(0)
                column = 0
                for letter in letters:
                    column = column * 26 + ord(letter) - 64
                values[column] = _cell_value(cell, shared)
            rows.append([values.get(index) for index in range(1, 13)])
        return rows


def build_catalog(source: Path) -> dict[str, object]:
    """Convierte exactamente las 254 filas y falla ante cambios de estructura o cardinalidad."""
    rows = read_rows(source)
    expected_functional_headers = [
        "ID", "Categoría principal", "Subcategoría", "Tipo de local normalizado",
    ]
    if (
        rows[0][:4] != expected_functional_headers
        or rows[0][8] != "Uso principal"
        or len(rows[0]) != 12
        or len(rows) != 255
    ):
        raise ValueError("VENUE_TAXONOMY_SOURCE_SHAPE_CHANGED")
    records = rows[1:]
    family_labels = list(dict.fromkeys(str(row[1]) for row in records))
    if set(family_labels) != set(FAMILY_I18N) or len(family_labels) != 23:
        raise ValueError("VENUE_TAXONOMY_SOURCE_FAMILIES_CHANGED")

    families = []
    for label in family_labels:
        english, definition_es, definition_en = FAMILY_I18N[label]
        families.append({
            "code": slug(label),
            "name": {"es": label, "en": english},
            "definition": {"es": definition_es, "en": definition_en},
            "governanceStatus": "candidate",
        })
    types = []
    cleaned_labels = {
        31: "Autoservicio pequeño (<120 m²)",
        32: "Superservicio (120-399 m²)",
        33: "Supermercado (≥400 m²)",
        108: "Gran superficie especializada (≥2.500 m²)",
    }
    for row in records:
        source_id = int(row[0])
        label = cleaned_labels.get(source_id, str(row[3]))
        types.append({
            "sourceId": source_id,
            "code": slug(label),
            "familyCode": slug(str(row[1])),
            "subcategoryCode": slug(str(row[2])),
            "name": {"es": label, "en": None},
            "sourceSubcategoryEs": row[2],
            "useCode": slug(str(row[8])),
            "useLabelEs": row[8],
            "translationStatus": "pendingHumanReview",
            "governanceStatus": "candidate",
        })
    compatibility = [
        {
            "legacyCategoryCode": code,
            "mappingKind": kind,
            "targetTypeCodes": targets,
            "operatorTypeCode": operator,
            "mappingStatus": status,
            "existingImagesReusableForDevelopment": True,
            "existingImagesEligibleAsNewTest": False,
            "humanRelabelReviewRequired": True,
        }
        for code, kind, targets, operator, status in LEGACY
    ]
    return {
        "schemaVersion": 1,
        "taxonomyVersion": "venue-taxonomy.v1",
        "effectiveFrom": "2026-08-30",
        "locales": ["es", "en"],
        "activationStatus": "candidateOnly",
        "source": {
            "fileSha256": hashlib.sha256(source.read_bytes()).hexdigest(),
            "sourceVersion": "2026-08-29",
            "recordCount": 254,
            "scopeEs": "Locales físicos de interés comercial, sanitario, profesional o de servicios; no representa todos los establecimientos posibles.",
        },
        "families": families,
        "types": types,
        "legacyCompatibility": compatibility,
    }


def run() -> None:
    """CLI explícito; nunca modifica el libro fuente."""
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    catalog = build_catalog(args.source)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    run()
