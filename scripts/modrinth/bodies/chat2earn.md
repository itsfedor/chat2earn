# Chat2Earn

Pays players server money for chatting in English. Every message is scored
by an AI model, and the payout scales with how good the English is.

Part of a gamified ESL teaching setup: chat is the lesson, money is the motivation. The other plugins live under the [ESL Automation Suite](https://github.com/itsfedor/esl-automation-suite).

## Requirements

- Vault (any Vault economy provider works, VaultUnlocked included)
- A free Groq API key from https://console.groq.com

## How it works

1. A player sends a chat message.
2. Filters reject spam: at least 3 words, less than 49% Cyrillic characters.
3. The message is scored by Groq (`llama-3.1-8b-instant`) for English quality.
4. The score becomes points: up to 4 per message, worth $0.10 each.
5. Vault pays out. Every $5.00 of earnings triggers a milestone message.

The AI call runs async, so chat never waits on the model.

## Install

1. Drop `Chat2Earn.jar` into your `plugins/` folder.
2. Copy `config.example.yml` from the [repo](https://github.com/itsfedor/chat2earn) to `config.yml`.
3. Put your Groq key in `config.yml`.
4. Restart the server.

## Configuration

```yaml
groq:
  api-key: "YOUR_GROQ_API_KEY"
  model: "llama-3.1-8b-instant"
  timeout-seconds: 10

scoring:
  points-per-message-max: 4
  dollars-per-point: 0.10
  milestone-dollars: 5.0

filters:
  min-words: 3
  max-cyrillic-percent: 49
```

`max-cyrillic-percent` is the anti-spam line. Students who mix languages
mid-sentence may score below it; raise it to 60 if that is your group.
Russian-only chat should stay blocked.

## Pair it with

[EnglishProgression](https://modrinth.com/plugin/englishprogression), which
watches lifetime earnings and promotes players up a LuckPerms track at set
thresholds. The two were designed as a pair.

## License

MIT. Source code is in the [repository](https://github.com/itsfedor/chat2earn).
