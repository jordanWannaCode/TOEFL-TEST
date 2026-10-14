"""Génère un override local pour le proxy cloud sans exposer ses paramètres."""
import json
import os
import socket
from pathlib import Path
from urllib.parse import urlsplit
from xml.sax.saxutils import escape

output = Path(os.environ.get('TOEFL_CLOUD_CONFIG_DIR', '/workspace/.tools/toefl-cloud'))
output.mkdir(parents=True, exist_ok=True)
proxy = urlsplit(os.environ['HTTPS_PROXY'])
if proxy.username or proxy.password:
    raise SystemExit('Proxy authentifié : configuration sécurisée supplémentaire nécessaire.')
assert proxy.hostname and proxy.port
ca = Path(os.environ['CODEX_PROXY_CERT'])
assert ca.is_file()
proxy_ip = socket.gethostbyname(proxy.hostname)
settings = output / 'maven-settings.xml'
settings.write_text(f'''<settings><proxies><proxy><id>cloud</id><active>true</active><protocol>https</protocol><host>{escape(proxy.hostname)}</host><port>{proxy.port}</port></proxy></proxies></settings>''')
settings.chmod(0o600)
args = {name: os.environ.get(name, '') for name in ['HTTP_PROXY', 'HTTPS_PROXY', 'NO_PROXY']}
services = {name: {'build': {'network': 'host', 'extra_hosts': [f'{proxy.hostname}:{proxy_ip}'], 'args': args, 'secrets': ['cloud_ca']}} for name in ['backend', 'frontend', 'ai-service', 'minio']}
services['backend']['build']['secrets'].append('cloud_maven_settings')
# Les tests Maven ont besoin du même proxy et du même certificat de confiance.
services['backend-check'] = {
    'extra_hosts': [f'{proxy.hostname}:{proxy_ip}'],
    'volumes': [f'{settings}:/run/cloud/maven-settings.xml:ro', f'{ca}:/run/cloud/ca.crt:ro'],
    'command': ['sh', '-c', 'if ! keytool -list -alias cloud-proxy -cacerts -storepass changeit >/dev/null 2>&1; then keytool -importcert -noprompt -alias cloud-proxy -file /run/cloud/ca.crt -cacerts -storepass changeit; fi && mvn -s /run/cloud/maven-settings.xml -B verify'],
}
config = {'services': services, 'secrets': {'cloud_ca': {'file': str(ca)}, 'cloud_maven_settings': {'file': str(settings)}}}
(output / 'compose.json').write_text(json.dumps(config, indent=2))
print('Override cloud créé hors dépôt ; aucune valeur affichée.')
