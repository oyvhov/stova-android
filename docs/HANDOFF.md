# Plan 4

- 2026-10-25: Arbeid på `plan-4-home-assistant` berre i denne worktreen. Oppgåve 1 fullført; oppgåve 2 under arbeid.
- Oppgåve 10 blir hoppa over etter brukarønske. Ingen ekte HA eller eining er testa.
- Avvik oppgåve 2: Plan 2 er ikkje implementert i denne greina, så OkHttp 4.12.0 og MockWebServer 4.12.0 blir lagde til her.
- Avvik oppgåve 2: Tilbakekalling bruker `/auth/token` med `action=revoke`, ikkje `/auth/revoke`, etter https://contributors.home-assistant.io/core/internals/auth-api/.
- Smarthus skal bruke eksisterande topplinje og fullskjermside; ingen sidemeny. Ingen push.

- Avvik oppgåve 3: `kidSmartHomeControl` er lagt til før oppgåve 6 fordi innstillingsbrytaren i oppgåve 3 treng feltet. INTERNET og cleartext-støtte er lagde til fordi Plan 2 ikkje er på denne greina; LAN-regelen blir handheva før adressa blir brukt.
