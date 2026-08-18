**TL;DR:** Diese Anleitung beschreibt, wie die `infisical_secrets`-Rolle aus diesem Template-Repo in ein Ansible-Projekt eingebunden wird, um Secrets aus Infisical zu laden.

## Tutorial ansible mit infisical

Die ganze doku kann auf : https://infisical.com/docs/integrations/platforms/ansible gefunden werden

## Installation

```
ansible-galaxy collection infisical.vault
pip install infisicalsdk
```

## Voraussetzungen

Beim Arbeiten mit Infisical müssen zwei Voraussetzungen erfüllt sein:

- Infisical muss installiert sein.
- Eine `infisical.json`-Datei muss erzeugt werden mit:

  ```bash
  infisical login
  infisical init
  ```

Für die genaue Referenz siehe das README in [infisicalplugin](https://gitlab.abuscom.cloud/fabian.santer/infisicalplugin).

2. Env-Dateien in Infisical hochladen, falls das noch nicht passiert ist.

3. Ausführen mit:

   ```bash
   infisical run --env=dev -- ansible-playbook playbook.yml -i "localhost,"
   ```

   Das Environment kann entweder über `"defaultEnvironment": ""` in der `infisical.json`-Datei oder direkt über die Flag `--env` gesteuert werden. Wichtig: hier muss 
   der enviroment slug angegeben werden der kann vom echten enviroment namen abweichen und man findet ihn unter settings > secrets management > Enviroments
4.  Wichtig ist ,dass wenn man diesen befehl ausführt immer in der CLI mit infisical eingelogged ist.
