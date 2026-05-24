import os
import sys
import zipfile

import yaml
from superset.app import create_app

new_uri = (
    f"clickhousedb+connect://{os.environ['CH_USER']}:{os.environ['CH_PASS']}"
    f"@{os.environ['HOST_CLICKHOUSE']}:8123/{os.environ['CH_DB']}"
)

contents = {}
with zipfile.ZipFile(sys.argv[1]) as zf:
    all_files = [n for n in zf.namelist() if not n.endswith('/')]
    prefix = (all_files[0].split('/')[0] + '/') if all_files and '/' in all_files[0] else ''
    if not all(n.startswith(prefix) for n in all_files):
        prefix = ''
    for name in zf.namelist():
        if name.endswith('/'):
            continue
        rel = name[len(prefix):]
        data = zf.read(name).decode('utf-8')
        if rel.startswith('databases/') and os.path.basename(rel).startswith('DWH_2.'):
            cfg = yaml.safe_load(data)
            cfg['sqlalchemy_uri'] = new_uri
            data = yaml.dump(cfg, allow_unicode=True)
        contents[rel] = data

app = create_app()
with app.app_context():
    with app.test_request_context():
        from flask import g
        from superset.extensions import appbuilder
        from superset.commands.dashboard.importers.dispatcher import ImportDashboardsCommand
        g.user = appbuilder.sm.find_user(username=os.environ['SUPERSET_ADMIN_USERNAME'])
        ImportDashboardsCommand(contents, overwrite=True).run()
