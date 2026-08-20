#!/usr/bin/env bash
# Uploads the custom plugins to Modrinth (create project + upload version).
#
# Usage:
#   MODRINTH_TOKEN=mrp_... ./scripts/modrinth-upload.sh            # all plugins
#   MODRINTH_TOKEN=mrp_... ./scripts/modrinth-upload.sh chat2earn  # one plugin
#
# Token: create one at https://modrinth.com/settings/pat with scopes
# "Create Project" and "Create Version".
set -euo pipefail
cd "$(dirname "$0")/.."

API="https://api.modrinth.com/v2"
TOKEN="${MODRINTH_TOKEN:?set MODRINTH_TOKEN (mrp_... from https://modrinth.com/settings/pat)}"
GAME_VERSIONS='["1.21","1.21.1","1.21.2","1.21.3","1.21.4","1.21.5","1.21.6","1.21.7","1.21.8","1.21.9","1.21.10","1.21.11"]'
LOADERS='["paper","spigot","bukkit"]'

if [ $# -gt 0 ]; then
  metas=()
  for s in "$@"; do metas+=("scripts/modrinth/$s.json"); done
else
  metas=(scripts/modrinth/*.json)
fi

for meta in "${metas[@]}"; do
  [ -f "$meta" ] || { echo "skip: $meta not found"; continue; }
  slug=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['slug'])" "$meta")
  jar="$slug.jar"
  [ -f "$jar" ] || { echo "skip: $jar not found"; continue; }
  echo "== $slug"

  code=$(curl -s -o /dev/null -w "%{http_code}" "$API/project/$slug")
  if [ "$code" = "404" ]; then
    echo "  creating project..."
    python3 - "$meta" > /tmp/modrinth-project.json <<'PY'
import json, sys
m = json.load(open(sys.argv[1]))
m["body"] = open(m.pop("body_file")).read()
print(json.dumps(m))
PY
    ICON_ARGS=()
  [ -f assets/server-icon.png ] && ICON_ARGS=(-F "icon=@assets/server-icon.png")
  curl -s -X POST "$API/project" -H "Authorization: Bearer $TOKEN" \
      -F "data=</tmp/modrinth-project.json" "${ICON_ARGS[@]}" \
      | python3 -c "import sys,json; r=json.load(sys.stdin); print('  created:', r.get('id') or r.get('error'))"
  else
    echo "  project already exists (HTTP $code), skipping creation"
  fi

  python3 - "$slug" > /tmp/modrinth-version.json <<'PY'
import json, sys
slug = sys.argv[1]
data = {
    "project_id": slug,
    "name": "1.0.0",
    "version_number": "1.0.0",
    "game_versions": json.loads("""$GAME_VERSIONS""".strip()),
    "loaders": json.loads("""$LOADERS""".strip()),
    "file_parts": ["plugin"],
    "primary_file": "plugin",
    "dependencies": [],
    "changelog": "First public release."
}
print(json.dumps(data))
PY
  curl -s -X POST "$API/version" -H "Authorization: Bearer $TOKEN" \
    -F "data=</tmp/modrinth-version.json" \
    -F "plugin=@$jar" \
    | python3 -c "import sys,json; r=json.load(sys.stdin); print('  version:', r.get('id') or r.get('error'))"
done

echo "done. Project pages: https://modrinth.com/plugin/<slug>"
