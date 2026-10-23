"""Génère docs/shade-review.html à partir de catalog.json, pour la relecture des fiches (AC14)."""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parent.parent
catalog = json.loads((root / "core/src/main/resources/catalog.json").read_text(encoding="utf-8"))
template = (root / "tools/shade-review.template.html").read_text(encoding="utf-8")
# "</" échappé pour qu'aucun texte du catalogue ne puisse fermer la balise <script>.
data = json.dumps(catalog, ensure_ascii=False).replace("</", "<\\/")
(root / "docs/shade-review.html").write_text(template.replace("__DATA__", data), encoding="utf-8")
print("docs/shade-review.html:", len(catalog["shades"]), "nuances")
