# Publishing scripts

This folder holds everything needed to publish the custom plugins
(Chat2Earn, EnglishProgression, VocabQuiz, DailyEnglish) to
[Modrinth](https://modrinth.com).

## One-time setup

1. Create a Modrinth account if you don't have one.
2. Go to https://modrinth.com/settings/pat and create a personal access
   token with the scopes **Create Project** and **Create Version**.
3. Save it as `MODRINTH_TOKEN` in `~/.hermes/.env`:

   ```
   MODRINTH_TOKEN=mrp_...
   ```

## Upload

```bash
MODRINTH_TOKEN=mrp_... ./scripts/modrinth-upload.sh            # all four plugins
MODRINTH_TOKEN=mrp_... ./scripts/modrinth-upload.sh chat2earn  # just one
```

The script creates each project (if the slug is free) and uploads a
`1.0.0` version with the prebuilt jar. Project pages land at
`https://modrinth.com/plugin/<slug>`.

## What the script does

- Reads `scripts/modrinth/<slug>.json` for project metadata
  (title, summary, categories, license, body)
- Reads the body markdown from `scripts/modrinth/bodies/<slug>.md`
- Creates the project via `POST /v2/project` (multipart, with the server
  icon as project icon)
- Uploads the version via `POST /v2/version` (multipart, jar as primary file)
- Game versions: 1.21 through 1.21.11, loaders: paper, spigot, bukkit

If the project slug already exists, the script skips creation and only
uploads the version. Re-running after a bump of `version_number` in the
script publishes a new release.

## Notes

- The Groq API key is never in the repo. Chat2Earn ships with
  `config.example.yml` and users add their own key.
- The jar at the repo root is the exact build running on the ESL
  English Server.
